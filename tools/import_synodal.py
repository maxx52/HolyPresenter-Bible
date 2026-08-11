#!/usr/bin/env python3
"""Convert the public-domain eBible Russian Synodal USFM archive to HolyPresenter JSON."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path


SOURCE_URL = "https://ebible.org/Scriptures/russyn_usfm.zip"
SOURCE_SHA256 = "acd5d80c0d28ca72d17cb12a2bb9f561d957439b03bddfaa433beb51cf7b0363"
EXPECTED_CHAPTERS = 1189
EXPECTED_VERSES = 31169


@dataclass(frozen=True)
class BookMetadata:
    usfm_code: str
    book_id: str
    name: str
    abbreviation: str
    testament: str


BOOKS = (
    BookMetadata("GEN", "genesis", "Бытие", "Быт", "OLD"),
    BookMetadata("EXO", "exodus", "Исход", "Исх", "OLD"),
    BookMetadata("LEV", "leviticus", "Левит", "Лев", "OLD"),
    BookMetadata("NUM", "numbers", "Числа", "Чис", "OLD"),
    BookMetadata("DEU", "deuteronomy", "Второзаконие", "Втор", "OLD"),
    BookMetadata("JOS", "joshua", "Иисус Навин", "Нав", "OLD"),
    BookMetadata("JDG", "judges", "Судьи", "Суд", "OLD"),
    BookMetadata("RUT", "ruth", "Руфь", "Руф", "OLD"),
    BookMetadata("1SA", "1-samuel", "1 Царств", "1 Цар", "OLD"),
    BookMetadata("2SA", "2-samuel", "2 Царств", "2 Цар", "OLD"),
    BookMetadata("1KI", "1-kings", "3 Царств", "3 Цар", "OLD"),
    BookMetadata("2KI", "2-kings", "4 Царств", "4 Цар", "OLD"),
    BookMetadata("1CH", "1-chronicles", "1 Паралипоменон", "1 Пар", "OLD"),
    BookMetadata("2CH", "2-chronicles", "2 Паралипоменон", "2 Пар", "OLD"),
    BookMetadata("EZR", "ezra", "Ездра", "Езд", "OLD"),
    BookMetadata("NEH", "nehemiah", "Неемия", "Неем", "OLD"),
    BookMetadata("EST", "esther", "Есфирь", "Есф", "OLD"),
    BookMetadata("JOB", "job", "Иов", "Иов", "OLD"),
    BookMetadata("PSA", "psalms", "Псалтирь", "Пс", "OLD"),
    BookMetadata("PRO", "proverbs", "Притчи", "Притч", "OLD"),
    BookMetadata("ECC", "ecclesiastes", "Екклесиаст", "Еккл", "OLD"),
    BookMetadata("SNG", "song-of-solomon", "Песнь Песней", "Песн", "OLD"),
    BookMetadata("ISA", "isaiah", "Исаия", "Ис", "OLD"),
    BookMetadata("JER", "jeremiah", "Иеремия", "Иер", "OLD"),
    BookMetadata("LAM", "lamentations", "Плач Иеремии", "Плач", "OLD"),
    BookMetadata("EZK", "ezekiel", "Иезекииль", "Иез", "OLD"),
    BookMetadata("DAN", "daniel", "Даниил", "Дан", "OLD"),
    BookMetadata("HOS", "hosea", "Осия", "Ос", "OLD"),
    BookMetadata("JOL", "joel", "Иоиль", "Иоил", "OLD"),
    BookMetadata("AMO", "amos", "Амос", "Ам", "OLD"),
    BookMetadata("OBA", "obadiah", "Авдий", "Авд", "OLD"),
    BookMetadata("JON", "jonah", "Иона", "Ион", "OLD"),
    BookMetadata("MIC", "micah", "Михей", "Мих", "OLD"),
    BookMetadata("NAM", "nahum", "Наум", "Наум", "OLD"),
    BookMetadata("HAB", "habakkuk", "Аввакум", "Авв", "OLD"),
    BookMetadata("ZEP", "zephaniah", "Софония", "Соф", "OLD"),
    BookMetadata("HAG", "haggai", "Аггей", "Агг", "OLD"),
    BookMetadata("ZEC", "zechariah", "Захария", "Зах", "OLD"),
    BookMetadata("MAL", "malachi", "Малахия", "Мал", "OLD"),
    BookMetadata("MAT", "matthew", "От Матфея", "Мф", "NEW"),
    BookMetadata("MRK", "mark", "От Марка", "Мк", "NEW"),
    BookMetadata("LUK", "luke", "От Луки", "Лк", "NEW"),
    BookMetadata("JHN", "john", "От Иоанна", "Ин", "NEW"),
    BookMetadata("ACT", "acts", "Деяния", "Деян", "NEW"),
    BookMetadata("ROM", "romans", "К Римлянам", "Рим", "NEW"),
    BookMetadata("1CO", "1-corinthians", "1 Коринфянам", "1 Кор", "NEW"),
    BookMetadata("2CO", "2-corinthians", "2 Коринфянам", "2 Кор", "NEW"),
    BookMetadata("GAL", "galatians", "К Галатам", "Гал", "NEW"),
    BookMetadata("EPH", "ephesians", "К Ефесянам", "Еф", "NEW"),
    BookMetadata("PHP", "philippians", "К Филиппийцам", "Флп", "NEW"),
    BookMetadata("COL", "colossians", "К Колоссянам", "Кол", "NEW"),
    BookMetadata("1TH", "1-thessalonians", "1 Фессалоникийцам", "1 Фес", "NEW"),
    BookMetadata("2TH", "2-thessalonians", "2 Фессалоникийцам", "2 Фес", "NEW"),
    BookMetadata("1TI", "1-timothy", "1 Тимофею", "1 Тим", "NEW"),
    BookMetadata("2TI", "2-timothy", "2 Тимофею", "2 Тим", "NEW"),
    BookMetadata("TIT", "titus", "К Титу", "Тит", "NEW"),
    BookMetadata("PHM", "philemon", "К Филимону", "Флм", "NEW"),
    BookMetadata("HEB", "hebrews", "К Евреям", "Евр", "NEW"),
    BookMetadata("JAS", "james", "Иакова", "Иак", "NEW"),
    BookMetadata("1PE", "1-peter", "1 Петра", "1 Пет", "NEW"),
    BookMetadata("2PE", "2-peter", "2 Петра", "2 Пет", "NEW"),
    BookMetadata("1JN", "1-john", "1 Иоанна", "1 Ин", "NEW"),
    BookMetadata("2JN", "2-john", "2 Иоанна", "2 Ин", "NEW"),
    BookMetadata("3JN", "3-john", "3 Иоанна", "3 Ин", "NEW"),
    BookMetadata("JUD", "jude", "Иуды", "Иуд", "NEW"),
    BookMetadata("REV", "revelation", "Откровение", "Откр", "NEW"),
)


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def parse_book(archive: zipfile.ZipFile, metadata: BookMetadata, order: int) -> dict:
    suffix = f"-{metadata.usfm_code}russyn.usfm"
    matches = [name for name in archive.namelist() if name.endswith(suffix)]
    if len(matches) != 1:
        raise ValueError(
            f"Expected one USFM file ending in {suffix}, found {len(matches)}"
        )

    source = archive.read(matches[0]).decode("utf-8-sig")
    chapters: list[dict] = []
    current_chapter: dict | None = None
    current_verse: dict | None = None

    def finish_verse() -> None:
        nonlocal current_verse
        if current_verse is None:
            return
        if current_chapter is None:
            raise ValueError(f"Verse outside a chapter in {metadata.usfm_code}")

        text = normalized_text(" ".join(current_verse.pop("parts")))
        if not text:
            raise ValueError(
                f"Empty verse {metadata.usfm_code} "
                f"{current_chapter['number']}:{current_verse['number']}"
            )
        current_verse["text"] = text
        current_chapter["verses"].append(current_verse)
        current_verse = None

    for raw_line in source.splitlines():
        line = raw_line.strip()

        chapter_match = re.match(r"^\\c\s+(\d+)", line)
        if chapter_match:
            finish_verse()
            current_chapter = {
                "number": int(chapter_match.group(1)),
                "verses": [],
            }
            chapters.append(current_chapter)
            continue

        verse_match = re.match(r"^\\v\s+(\d+)\s*(.*)$", line)
        if verse_match:
            finish_verse()
            if current_chapter is None:
                raise ValueError(f"Verse before a chapter in {metadata.usfm_code}")
            current_verse = {
                "number": int(verse_match.group(1)),
                "parts": [verse_match.group(2)],
            }
            continue

        continuation_match = re.match(r"^\\(?:m|q1)\s*(.*)$", line)
        if continuation_match and current_verse is not None:
            current_verse["parts"].append(continuation_match.group(1))

    finish_verse()

    chapter_numbers = [chapter["number"] for chapter in chapters]
    if chapter_numbers != sorted(set(chapter_numbers)):
        raise ValueError(f"Invalid chapter order in {metadata.usfm_code}")

    for chapter in chapters:
        verse_numbers = [verse["number"] for verse in chapter["verses"]]
        if verse_numbers != sorted(set(verse_numbers)):
            raise ValueError(
                f"Invalid verse order in {metadata.usfm_code} {chapter['number']}"
            )

    return {
        "id": metadata.book_id,
        "name": metadata.name,
        "abbreviation": metadata.abbreviation,
        "testament": metadata.testament,
        "order": order,
        "chapters": chapters,
    }


def build_translation(source_archive: Path) -> dict:
    digest = hashlib.sha256(source_archive.read_bytes()).hexdigest()
    if digest != SOURCE_SHA256:
        raise ValueError(
            "Unexpected source archive SHA-256. Review the updated eBible source "
            "before changing SOURCE_SHA256."
        )

    with zipfile.ZipFile(source_archive) as archive:
        books = [
            parse_book(archive, metadata, order)
            for order, metadata in enumerate(BOOKS, start=1)
        ]

    chapter_count = sum(len(book["chapters"]) for book in books)
    verse_count = sum(
        len(chapter["verses"])
        for book in books
        for chapter in book["chapters"]
    )

    if chapter_count != EXPECTED_CHAPTERS or verse_count != EXPECTED_VERSES:
        raise ValueError(
            f"Unexpected corpus size: {len(books)} books, "
            f"{chapter_count} chapters, {verse_count} verses"
        )

    return {
        "id": "synodal",
        "name": "Синодальный перевод",
        "abbreviation": "Синодальный",
        "language": "ru",
        "books": books,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Convert the eBible Russian Synodal USFM archive to HolyPresenter JSON. "
            f"Download the source from {SOURCE_URL} first."
        )
    )
    parser.add_argument("source_archive", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    translation = build_translation(args.source_archive)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as output_file:
        json.dump(
            translation,
            output_file,
            ensure_ascii=False,
            separators=(",", ":"),
        )
        output_file.write("\n")

    chapter_count = sum(len(book["chapters"]) for book in translation["books"])
    verse_count = sum(
        len(chapter["verses"])
        for book in translation["books"]
        for chapter in book["chapters"]
    )
    print(
        f"Wrote {args.output}: {len(translation['books'])} books, "
        f"{chapter_count} chapters, {verse_count} verses"
    )


if __name__ == "__main__":
    main()
