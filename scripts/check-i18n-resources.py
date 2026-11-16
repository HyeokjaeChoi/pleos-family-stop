#!/usr/bin/env python3
"""Gate check (#94): per module, res/values (ko, default) and res/values-en (en) must agree.

- same string/plurals keys (translatable="false" skipped)
- same placeholders (position -> conversion type), e.g. %d, %1$s
- plurals: ko has `other`; en has `one` and `other`
Usage: scripts/check-i18n-resources.py [--self-test]   (exit 1 on any problem; passes when there are no res dirs)
"""
import os
import re
import sys
import tempfile
import xml.etree.ElementTree as ET

PH = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z])")


def placeholders(text):
    out, auto = {}, 0
    for m in PH.finditer(text.replace("%%", "")):
        if m.group(1):
            out[int(m.group(1))] = m.group(2)
        else:
            auto += 1
            out[auto] = m.group(2)
    return out


def load(res_dir, qualifier, errors):
    """-> {key: {quantity-or-'': placeholders}} for res/<qualifier>/*.xml"""
    entries = {}
    d = os.path.join(res_dir, qualifier)
    if not os.path.isdir(d):
        return entries
    for f in sorted(os.listdir(d)):
        if not f.endswith(".xml"):
            continue
        try:
            root = ET.parse(os.path.join(d, f)).getroot()
        except ET.ParseError as e:
            errors.append(f"{d}/{f}: {e}")
            continue
        for el in root:
            if el.get("translatable") == "false" or el.get("name") is None:
                continue
            if el.tag == "string":
                entries[el.get("name")] = {"": placeholders("".join(el.itertext()))}
            elif el.tag == "plurals":
                entries[el.get("name")] = {
                    i.get("quantity"): placeholders("".join(i.itertext())) for i in el.findall("item")
                }
    return entries


def check_res(res_dir):
    errors = []
    ko, en = load(res_dir, "values", errors), load(res_dir, "values-en", errors)
    for k in sorted(set(ko) | set(en)):
        if k not in en:
            errors.append(f"{res_dir}: '{k}' missing in values-en")
            continue
        if k not in ko:
            errors.append(f"{res_dir}: '{k}' missing in values")
            continue
        kq, eq = ko[k], en[k]
        if ("" in kq) != ("" in eq):
            errors.append(f"{res_dir}: '{k}' is string in one locale and plurals in the other")
            continue
        if "" in kq:
            if kq[""] != eq[""]:
                errors.append(f"{res_dir}: '{k}' placeholders differ: ko={kq['']} en={eq['']}")
            continue
        if "other" not in kq:
            errors.append(f"{res_dir}: plurals '{k}' in values needs quantity 'other'")
        for q in ("one", "other"):
            if q not in eq:
                errors.append(f"{res_dir}: plurals '{k}' in values-en needs quantity '{q}'")
        if "other" in kq and "other" in eq and kq["other"] != eq["other"]:
            errors.append(f"{res_dir}: plurals '{k}' placeholders differ: ko={kq['other']} en={eq['other']}")
    return errors


def res_dirs(root):
    for dp, dns, _ in os.walk(root):
        dns[:] = [d for d in dns if d not in ("build", ".git", ".gradle", "node_modules")]
        if os.path.basename(dp) == "res" and os.path.isdir(os.path.join(dp, "values")):
            yield dp
        if os.path.basename(dp) == "res":
            dns[:] = []


def write(path, body):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(f"<resources>{body}</resources>")


def self_test():
    ok = '<string name="a">%1$s · %2$d</string><plurals name="p"><item quantity="{q}">%d</item>{x}</plurals>'
    with tempfile.TemporaryDirectory() as t:
        res = os.path.join(t, "m/src/main/res")
        write(res + "/values/s.xml", ok.format(q="other", x=""))
        good_en = ok.format(q="one", x='<item quantity="other">%d</item>')
        write(res + "/values-en/s.xml", good_en)
        assert check_res(res) == [], check_res(res)
        cases = {
            "missing key": '<string name="b">x</string>' + good_en,
            "placeholder type": good_en.replace("%2$d", "%2$s"),
            "placeholder count": good_en.replace(" · %2$d", ""),
            "plural one": ok.format(q="other", x=""),
        }
        for name, en in cases.items():
            write(res + "/values-en/s.xml", en)
            assert check_res(res), f"self-test: '{name}' not detected"
    print("i18n-resources self-test: OK")


def main():
    if "--self-test" in sys.argv:
        return self_test()
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    errors = [e for r in res_dirs(".") for e in check_res(r)]
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    print("i18n-resources check: OK")


if __name__ == "__main__":
    main()
