"""Change only the exact manifest package string in a UTF-16 Android binary XML APK.

This deliberately does not rename Java classes or provider authorities. It is a
diagnostic repack, not a production-safe application-id migration. The output
is unsigned and must be zipaligned and signed before installation.
"""

import argparse
import re
import struct
import zipfile


def patch_manifest(data: bytes, old: str, new: str) -> bytes:
    if len(old.encode("utf-16le")) != len(new.encode("utf-16le")):
        raise ValueError("Replacement package must have the same UTF-16 length")
    if struct.unpack_from("<H", data, 0)[0] != 3:
        raise ValueError("Not an Android binary XML manifest")
    position = 8
    while position < len(data):
        chunk_type, _, size = struct.unpack_from("<HHI", data, position)
        if size < 8 or position + size > len(data):
            raise ValueError("Invalid binary XML chunk")
        if chunk_type == 1:
            count, _, flags, strings_start, _ = struct.unpack_from("<IIIII", data, position + 8)
            if flags & 0x100:
                raise ValueError("UTF-8 string pool not supported by this conservative tool")
            result = bytearray(data)
            matches = 0
            for index in range(count):
                offset = struct.unpack_from("<I", data, position + 28 + 4 * index)[0]
                length_at = position + strings_start + offset
                length = struct.unpack_from("<H", data, length_at)[0]
                if length & 0x8000:
                    length = ((length & 0x7FFF) << 16) | struct.unpack_from("<H", data, length_at + 2)[0]
                    text_at = length_at + 4
                else:
                    text_at = length_at + 2
                if data[text_at : text_at + length * 2].decode("utf-16le") == old:
                    result[text_at : text_at + length * 2] = new.encode("utf-16le")
                    matches += 1
            if matches != 1:
                raise ValueError(f"Expected one exact manifest package string, found {matches}")
            return bytes(result)
        position += size
    raise ValueError("Manifest string pool not found")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("unsigned_output")
    parser.add_argument("old_package")
    parser.add_argument("new_package")
    args = parser.parse_args()
    with zipfile.ZipFile(args.source) as source, zipfile.ZipFile(args.unsigned_output, "w") as target:
        for entry in source.infolist():
            name = entry.filename
            if re.fullmatch(r"META-INF/[^/]+\.(?:MF|SF|RSA|DSA|EC)", name, re.IGNORECASE):
                continue
            content = source.read(name)
            if name == "AndroidManifest.xml":
                content = patch_manifest(content, args.old_package, args.new_package)
            target.writestr(entry, content)


if __name__ == "__main__":
    main()
