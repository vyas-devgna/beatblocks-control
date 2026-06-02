import json
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

GAME_VERSIONS = [
    "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
    "26.1", "26.1.1", "26.1.2",
    "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
]


def fetch_json(url: str):
    with urllib.request.urlopen(url, timeout=60) as resp:
        return json.load(resp)


def latest_yarn(game: str) -> str:
    data = fetch_json(f"https://meta.fabricmc.net/v2/versions/yarn/{game}")
    return data[0]["version"]


def latest_fabric_api(game: str) -> str:
    xml = urllib.request.urlopen(
        "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml",
        timeout=60,
    ).read()
    root = ET.fromstring(xml)
    versions = [v.text for v in root.findall(".//version") if v.text and f"+{game}" in v.text]
    if not versions:
        raise RuntimeError(f"No fabric-api for {game}")
    return versions[-1]


targets = []
for game in GAME_VERSIONS:
    try:
        yarn = latest_yarn(game)
        fabric = latest_fabric_api(game)
        targets.append({"Minecraft": game, "Yarn": yarn, "Fabric": fabric})
        print(f"OK {game} yarn={yarn} fabric={fabric}")
    except Exception as exc:
        print(f"SKIP {game}: {exc}")

out = Path(__file__).resolve().parent / "fabric-targets.json"
out.write_text(json.dumps(targets, indent=2), encoding="utf-8")
print(f"Wrote {len(targets)} targets to {out}")