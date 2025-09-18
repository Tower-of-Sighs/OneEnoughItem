import struct
import os
from textwrap import indent


def read_utf(f):
    """读取 Java UTF-8 字符串"""
    length_bytes = f.read(2)
    if len(length_bytes) < 2:
        raise EOFError("Unexpected end of file while reading UTF length")
    length = struct.unpack(">H", length_bytes)[0]
    if length == 0:
        return ""
    data = f.read(length)
    if len(data) < length:
        raise EOFError("Unexpected end of file while reading UTF data")
    return data.decode("utf-8")


def read_string_map(f):
    """读取 Java Map<String,String>"""
    count_bytes = f.read(4)
    if len(count_bytes) < 4:
        raise EOFError("Unexpected end of file while reading map size")
    count = struct.unpack(">I", count_bytes)[0]
    result = {}
    for _ in range(count):
        key = read_utf(f)
        value = read_utf(f)
        result[key] = value
    return result


def pretty_print_map(title, data, max_items=5):
    """格式化输出 Map，截断过长内容"""
    preview = dict(list(data.items())[:max_items])
    suffix = " ..." if len(data) > max_items else ""
    print(f"{title:<20}: {len(data)} items{suffix}")
    if preview:
        for k, v in preview.items():
            print(indent(f"{k} → {v}", "  "))


# ==================== 读取 Editor Cache ====================

def read_editor_cache(path):
    if not os.path.exists(path):
        print(f"[ERROR] 文件不存在: {path}")
        return

    with open(path, "rb") as f:
        try:
            version = struct.unpack(">I", f.read(4))[0]
            print(f"Editor Cache Version     : {version}")

            num_items = struct.unpack(">I", f.read(4))[0]
            match_items = [read_utf(f) for _ in range(num_items)]
            print(f"Match Items              : {match_items or '<none>'}")

            num_tags = struct.unpack(">I", f.read(4))[0]
            match_tags = [read_utf(f) for _ in range(num_tags)]
            print(f"Match Tags               : {match_tags or '<none>'}")

            result_item = read_utf(f)
            print(f"Result Item              : {result_item or '<none>'}")

            result_tag = read_utf(f)
            print(f"Result Tag               : {result_tag or '<none>'}")

            file_name = read_utf(f)
            print(f"File Name                : {file_name or '<none>'}")

        except EOFError:
            print("[ERROR] 文件内容不完整（可能是旧版本缓存或已损坏）")
        except Exception as e:
            print(f"[ERROR] 读取 editor_cache 时出错: {e}")


# ==================== 读取 Global Replacement Cache ====================

def read_global_replacement_cache(path):
    if not os.path.exists(path):
        print(f"[ERROR] 文件不存在: {path}")
        return

    with open(path, "rb") as f:
        try:
            version = struct.unpack(">I", f.read(4))[0]
            print(f"\nGlobal Replacement Cache Version: {version}")

            replaced_items = read_string_map(f)
            replaced_tags = read_string_map(f)

            try:
                result_items = read_string_map(f)
                result_tags = read_string_map(f)
            except EOFError:
                result_items, result_tags = {}, {}
                print("Warning: Could not read result tracking (可能是旧格式)")

            pretty_print_map("Replaced Items", replaced_items)
            pretty_print_map("Replaced Tags", replaced_tags)
            pretty_print_map("Result Items", result_items)
            pretty_print_map("Result Tags", result_tags)

        except EOFError:
            print("[ERROR] 文件内容不完整（可能是旧版本缓存或已损坏）")
        except Exception as e:
            print(f"[ERROR] 读取 global_replacement_cache 时出错: {e}")



if __name__ == "__main__":
    editor_cache_path = r"F:\code\mcmod\project\OneEnoughItem\run\oei\editor_cache.dat"
    global_cache_path = r"F:\code\mcmod\project\OneEnoughItem\run\oei\global_replacement_cache.dat"

    print("=== 读取 Editor Cache ===")
    read_editor_cache(editor_cache_path)

    print("\n=== 读取 Global Replacement Cache ===")
    read_global_replacement_cache(global_cache_path)
