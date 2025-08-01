## OneEnoughItem

### 概述
OneEnoughItem 模组的可视化编辑界面教学。

### 开始
按下绑定的快捷键（默认为 Ctrl + R）打开可视化编辑界面 

---

若还没创建数据包，在左边的输入框输入你想创建的数据包名称（可留空，默认为OEI），右边的输入框中输入你想创建的 json 文件名称，输入完成后点击创建文件按钮，就会自动在

> <存档名称>/datapacks/<数据包名称>/data/oneenoughitem/oneenoughitem/replacements

文件夹下创建数据包json文件。
![img.png](pic/img.png)

---

若有数据包，则可以点击选取文件按钮，点击你想进行操作的文件条目，会有三个按钮：

- **添加**：顾名思义添加内容；
- **更改**：进行这项操作，编辑界面左上角会有选择数组元素按钮，若数据包有多个json数组：
```json
[
    {
        "matchItems": [
            "#forge:ores"
        ],
        "resultItems": "minecraft:egg"
    },
    {
        "matchItems": [
            "minecraft:stone"
        ],
        "resultItems": "minecraft:air"
    }
]
```
此文件分两个部分，第一部分是数组序列1， 第二部分就是数组序列2。以此类推，选择你想更改的部分可随意修改保存。

---

应该也没什么介绍的了，剩下几个注意事项：

- 记得常按 Ctrl + S 进行缓存处理，这样退出界面/切换界面什么的不会丢失数据。
- 界面尺寸最好用4及以上，不然有些东西会点不到。