(() => {
    const $ = (id) => document.getElementById(id);

    const state = {
        replacements: [],
        index: 0,
        datapackPath: "",
        fileName: "",
        autoSave: false,
        saveTimeout: null,
        currentLang: "zh",
        langData: {}
    };

    // 多语言系统
    async function loadLanguage(lang) {
        try {
            const response = await fetch(`/web/lang/${lang}.json`);
            const data = await response.json();
            state.langData = data;
            state.currentLang = lang;
            updateUI();
            localStorage.setItem('oei-web-lang', lang);
        } catch (e) {
            console.error('Failed to load language:', e);
        }
    }

    function t(key, ...args) {
        let text = state.langData[key] || key;
        args.forEach((arg, index) => {
            text = text.replace(`{${index}}`, arg);
        });
        return text;
    }

    function updateUI() {
        // 更新所有带 data-i18n 属性的元素
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const args = el.getAttribute('data-i18n-args');
            if (args) {
                const argArray = args.split(',').map(s => s.trim());
                if (key === 'rule_count') {
                    const ruleCountEl = $("ruleCount");
                    el.innerHTML = t(key, ruleCountEl ? ruleCountEl.textContent || "0" : "0");
                } else {
                    el.textContent = t(key, ...argArray);
                }
            } else {
                el.textContent = t(key);
            }
        });

        // 更新 placeholder
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = t(key);
        });

        // 更新页面标题
        document.title = t('title');
    }

    function setStatus(text, ok = true, loading = false) {
        const el = $("status");
        if (el) {
            el.textContent = text || "";
            el.style.color = ok ? "#aaa" : "#f6685e";
            el.className = loading ? "status loading" : "status";
        }
    }

    function setScanStatus(text, ok = true, loading = false) {
        const el = $("scanStatus");
        if (el) {
            el.textContent = text || "";
            el.style.color = ok ? "#aaa" : "#f6685e";
            el.className = loading ? "status loading" : "status";
        }
    }

    function setSaveStatus(text, ok = true, loading = false) {
        const el = $("saveStatus");
        if (el) {
            el.textContent = text || "";
            el.style.color = ok ? "#aaa" : "#f6685e";
            el.className = loading ? "status loading" : "status";
        }
    }

    function showSection(sectionId, show = true) {
        const section = $(sectionId);
        if (section) {
            if (show) {
                section.style.display = "block";
                section.classList.add("fade-in");
                setTimeout(() => section.classList.remove("fade-in"), 400);
            } else {
                section.style.display = "none";
            }
        }
    }

    function sanitizeFileName(name) {
        return name.endsWith(".json") ? name : name + ".json";
    }

    async function scanFiles() {
        const pathEl = $("datapackPath");
        if (!pathEl) return;

        const path = pathEl.value.trim();
        if (!path) {
            setScanStatus(t("please_fill_path"), false);
            return;
        }

        setScanStatus(t("scanning"), true, true);
        const scanBtn = $("scanBtn");
        const fileSelect = $("fileSelect");
        const loadBtn = $("loadBtn");

        if (scanBtn) scanBtn.disabled = true;
        if (fileSelect) fileSelect.disabled = true;
        if (loadBtn) loadBtn.disabled = true;

        try {
            const url = `/api/scanFiles?datapackPath=${encodeURIComponent(path)}`;
            const res = await fetch(url);
            const json = await res.json();

            if (!json.ok) {
                setScanStatus(t("scan_failed", json.error || res.status), false);
                return;
            }

            if (fileSelect) {
                fileSelect.innerHTML = "";

                if (json.files.length === 0) {
                    const opt = document.createElement("option");
                    opt.value = "";
                    opt.textContent = t("no_json_files");
                    fileSelect.appendChild(opt);
                    setScanStatus(t("no_json_files"), false);
                } else {
                    const defaultOpt = document.createElement("option");
                    defaultOpt.value = "";
                    defaultOpt.textContent = t("please_select_file");
                    fileSelect.appendChild(defaultOpt);

                    json.files.forEach(file => {
                        const opt = document.createElement("option");
                        opt.value = file.name;
                        const countText = file.count >= 0 ? file.count + t("rules_count_suffix") : t("format_error");
                        opt.textContent = `${file.displayName} (${countText})`;
                        if (file.count < 0) opt.disabled = true;
                        fileSelect.appendChild(opt);
                    });

                    setScanStatus(t("files_found", json.files.length));
                    fileSelect.disabled = false;

                    fileSelect.onchange = () => {
                        if (loadBtn) loadBtn.disabled = !fileSelect.value;
                        if (fileSelect.value) {
                            state.fileName = fileSelect.value;
                        }
                    };
                }
            }
        } catch (e) {
            console.error(e);
            setScanStatus(t("scan_failed", e.message), false);
        } finally {
            if (scanBtn) scanBtn.disabled = false;
        }
    }

    function renderIndexSelect() {
        const sel = $("indexSelect");
        if (!sel) return;

        sel.innerHTML = "";
        for (let i = 0; i < state.replacements.length; i++) {
            const opt = document.createElement("option");
            opt.value = String(i);
            opt.textContent = t("index", i);
            if (i === state.index) opt.selected = true;
            sel.appendChild(opt);
        }
        sel.onchange = () => {
            state.index = parseInt(sel.value, 10) || 0;
            renderRules();
        };

        // 安全地更新规则计数
        const ruleCountEl = $("ruleCount");
        if (ruleCountEl) {
            ruleCountEl.textContent = state.replacements.length;
        }

        // 更新规则计数显示
        const ruleCountDisplayEl = document.querySelector('[data-i18n="rule_count"]');
        if (ruleCountDisplayEl) {
            ruleCountDisplayEl.innerHTML = t("rule_count", state.replacements.length);
        }
    }

    function getCurrentItem() {
        if (!Array.isArray(state.replacements) || state.replacements.length === 0) return null;
        if (state.index < 0 || state.index >= state.replacements.length) return null;
        return state.replacements[state.index];
    }

    function renderRuleChips(containerId, mapObj, removeCb) {
        const box = $(containerId);
        if (!box) return;

        box.innerHTML = "";
        if (!mapObj) return;
        Object.entries(mapObj).forEach(([k, v]) => {
            const chip = document.createElement("span");
            chip.className = "rule-chip";
            const key = document.createElement("span");
            key.className = "key";
            key.textContent = k;
            const mode = document.createElement("span");
            mode.className = "mode " + v;
            mode.textContent = v;
            const btn = document.createElement("button");
            btn.textContent = "×";
            btn.onclick = () => {
                removeCb(k);
                triggerAutoSave();
            };
            chip.appendChild(key);
            chip.appendChild(mode);
            chip.appendChild(btn);
            box.appendChild(chip);
        });
    }

    function renderRules() {
        const item = getCurrentItem();
        if (!item) {
            renderRuleChips("dataRules", null, () => {
            });
            renderRuleChips("tagRules", null, () => {
            });
            return;
        }
        if (!item.rules) item.rules = {};
        if (!item.rules.data) item.rules.data = {};
        if (!item.rules.tag) item.rules.tag = {};
        renderRuleChips("dataRules", item.rules.data, (k) => {
            delete item.rules.data[k];
            renderRules();
        });
        renderRuleChips("tagRules", item.rules.tag, (k) => {
            delete item.rules.tag[k];
            renderRules();
        });
    }

    async function loadData() {
        setStatus(t("loading"), true, true);
        setSaveStatus("");

        const datapackPathEl = $("datapackPath");
        const fileSelectEl = $("fileSelect");

        if (!datapackPathEl || !fileSelectEl) {
            setStatus(t("page_elements_missing"), false);
            return;
        }

        state.datapackPath = datapackPathEl.value.trim();
        state.fileName = fileSelectEl.value.trim();

        if (!state.datapackPath || !state.fileName) {
            setStatus(t("please_select_path_and_file"), false);
            return;
        }

        const loadBtn = $("loadBtn");
        if (loadBtn) loadBtn.disabled = true;

        const url = `/api/load?datapackPath=${encodeURIComponent(state.datapackPath)}&fileName=${encodeURIComponent(state.fileName)}`;
        try {
            const res = await fetch(url);
            const json = await res.json();
            if (!json.ok) {
                setStatus(t("load_failed", json.error || res.status), false);
                return;
            }
            state.replacements = json.replacements || [];
            state.index = 0;
            renderIndexSelect();
            renderRules();
            setStatus(t("rules_loaded", json.count));

            showSection("indexSection");
            showSection("rulesSection");
            showSection("saveSection");
        } catch (e) {
            console.error(e);
            setStatus(t("request_failed", e.message), false);
        } finally {
            if (loadBtn) loadBtn.disabled = false;
        }
    }

    function triggerAutoSave() {
        if (!state.autoSave) return;

        if (state.saveTimeout) {
            clearTimeout(state.saveTimeout);
        }

        state.saveTimeout = setTimeout(() => {
            saveCurrentRules(true);
        }, 1000);
    }

    async function saveCurrentRules(isAutoSave = false) {
        const item = getCurrentItem();
        if (!item) {
            setSaveStatus(t("no_saveable_index"), false);
            return;
        }
        const body = {
            datapackPath: state.datapackPath,
            fileName: sanitizeFileName(state.fileName),
            index: state.index,
            rules: item.rules || {}
        };

        setSaveStatus(isAutoSave ? t("auto_saving") : t("saving"), true, true);

        try {
            const res = await fetch("/api/saveRules", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(body)
            });
            const json = await res.json();
            if (!json.ok) {
                setSaveStatus(t("save_failed", json.error || res.status), false);
                return;
            }
            setSaveStatus(isAutoSave ? t("auto_saved") : t("saved"));

            // 成功保存后的视觉反馈
            const saveSection = $("saveSection");
            if (saveSection) {
                saveSection.classList.add("success-flash");
                setTimeout(() => saveSection.classList.remove("success-flash"), 600);
            }

        } catch (e) {
            console.error(e);
            setSaveStatus(t("request_failed", e.message), false);
        }
    }

    function bindAddButtons() {
        const addDataRuleBtn = $("addDataRule");
        const addTagRuleBtn = $("addTagRule");
        const dataKeyEl = $("dataKey");
        const tagKeyEl = $("tagKey");
        const dataModeEl = $("dataMode");
        const tagModeEl = $("tagMode");

        if (addDataRuleBtn) {
            addDataRuleBtn.onclick = () => {
                const item = getCurrentItem();
                if (!item || !dataKeyEl || !dataModeEl) return;
                const k = dataKeyEl.value.trim();
                const v = dataModeEl.value.trim();
                if (!k) return;
                item.rules ??= {};
                item.rules.data ??= {};
                item.rules.data[k] = v;
                dataKeyEl.value = "";
                renderRules();
                triggerAutoSave();
            };
        }

        if (addTagRuleBtn) {
            addTagRuleBtn.onclick = () => {
                const item = getCurrentItem();
                if (!item || !tagKeyEl || !tagModeEl) return;
                const k = tagKeyEl.value.trim();
                const v = tagModeEl.value.trim();
                if (!k) return;
                item.rules ??= {};
                item.rules.tag ??= {};
                item.rules.tag[k] = v;
                tagKeyEl.value = "";
                renderRules();
                triggerAutoSave();
            };
        }

        // 回车键快速添加
        if (dataKeyEl && addDataRuleBtn) {
            dataKeyEl.addEventListener("keypress", (e) => {
                if (e.key === "Enter") addDataRuleBtn.click();
            });
        }
        if (tagKeyEl && addTagRuleBtn) {
            tagKeyEl.addEventListener("keypress", (e) => {
                if (e.key === "Enter") addTagRuleBtn.click();
            });
        }
    }

    function bindLanguageButtons() {
        const langZhBtn = $("langZh");
        const langEnBtn = $("langEn");

        if (langZhBtn) {
            langZhBtn.onclick = () => {
                loadLanguage("zh");
                langZhBtn.classList.add("active");
                if (langEnBtn) langEnBtn.classList.remove("active");
            };
        }

        if (langEnBtn) {
            langEnBtn.onclick = () => {
                loadLanguage("en");
                if (langZhBtn) langZhBtn.classList.remove("active");
                langEnBtn.classList.add("active");
            };
        }
    }

    function init() {
        // 加载保存的语言设置
        const savedLang = localStorage.getItem('oei-web-lang') || 'zh';
        loadLanguage(savedLang);

        const langZhBtn = $("langZh");
        const langEnBtn = $("langEn");

        if (savedLang === 'en') {
            if (langEnBtn) langEnBtn.classList.add("active");
            if (langZhBtn) langZhBtn.classList.remove("active");
        } else {
            if (langZhBtn) langZhBtn.classList.add("active");
            if (langEnBtn) langEnBtn.classList.remove("active");
        }

        const scanBtn = $("scanBtn");
        const loadBtn = $("loadBtn");
        const saveBtn = $("saveBtn");
        const autoSaveEl = $("autoSave");
        const datapackPathEl = $("datapackPath");

        if (scanBtn) scanBtn.onclick = scanFiles;
        if (loadBtn) loadBtn.onclick = loadData;
        if (saveBtn) saveBtn.onclick = () => saveCurrentRules(false);

        if (autoSaveEl) {
            autoSaveEl.onchange = (e) => {
                state.autoSave = e.target.checked;
                if (state.autoSave) {
                    setSaveStatus(t("auto_save_enabled"));
                } else {
                    setSaveStatus("");
                    if (state.saveTimeout) {
                        clearTimeout(state.saveTimeout);
                        state.saveTimeout = null;
                    }
                }
            };
        }

        bindAddButtons();
        bindLanguageButtons();

        // 数据包路径输入时重置文件选择
        if (datapackPathEl) {
            datapackPathEl.addEventListener("input", () => {
                const fileSelect = $("fileSelect");
                const loadBtn = $("loadBtn");

                if (fileSelect) {
                    fileSelect.innerHTML = `<option value="">${t("please_scan_first")}</option>`;
                    fileSelect.disabled = true;
                }
                if (loadBtn) loadBtn.disabled = true;

                setScanStatus("");
                setStatus("");
                showSection("indexSection", false);
                showSection("rulesSection", false);
                showSection("saveSection", false);
            });
        }
    }

    document.addEventListener("DOMContentLoaded", init);
})();