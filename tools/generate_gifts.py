#!/usr/bin/env python3
"""Generates finder/src/main/assets/gifts.json - demo gift ideas with approximate local price ranges.

Prices are NOT real store prices. Each gift template has a base range (roughly USD); it is scaled by a
rough local purchasing-power factor, jittered a little per country/gift, and rounded to "nice" local numbers.
Run:  python3 tools/generate_gifts.py
"""
import json, math, os, random

# code: (country, currencyCode, symbol, factor, rounding)
COUNTRIES = [
    ("Philippines", "PHP", "₱", 22, "php"),
    ("United States", "USD", "$", 1.0, "unit"),
    ("South Korea", "KRW", "₩", 1000, "krw"),
    ("Japan", "JPY", "¥", 110, "jpy"),
    ("United Kingdom", "GBP", "£", 0.8, "unit"),
    ("Canada", "CAD", "$", 1.3, "unit"),
    ("Australia", "AUD", "$", 1.4, "unit"),
    ("Singapore", "SGD", "$", 1.3, "unit"),
    ("Thailand", "THB", "฿", 28, "ten"),
    ("Indonesia", "IDR", "Rp", 11000, "idr"),
    ("Malaysia", "MYR", "RM", 3.5, "unit"),
    ("India", "INR", "₹", 55, "ten"),
    ("Germany", "EUR", "€", 0.9, "unit"),
    ("France", "EUR", "€", 0.9, "unit"),
    ("Italy", "EUR", "€", 0.9, "unit"),
    ("Spain", "EUR", "€", 0.85, "unit"),
    ("Netherlands", "EUR", "€", 0.95, "unit"),
    ("Brazil", "BRL", "R$", 4.5, "unit"),
    ("Mexico", "MXN", "$", 15, "five"),
    ("New Zealand", "NZD", "$", 1.5, "unit"),
    ("United Arab Emirates", "AED", "د.إ", 3.6, "unit"),
]

# name, (lo, hi), recipients, categories, icon, description
T = [
    ("Mug", (4, 9), ["Anyone"], ["Useful", "Cute", "Christmas"], "🥛", "A practical and popular exchange gift."),
    ("Mini Scented Candle", (4, 9), ["Friend", "Coworker", "Woman"], ["Cute", "Christmas"], "🕯️", "A relaxing and affordable Christmas exchange gift."),
    ("Christmas Socks", (3, 8), ["Anyone"], ["Christmas", "Cute", "Funny"], "🧦", "Warm, festive and fun to unwrap."),
    ("Chocolate Gift Pack", (5, 12), ["Anyone"], ["Food", "Christmas"], "🍫", "A sweet treat almost everyone enjoys."),
    ("Keychain Set", (2, 6), ["Anyone"], ["Cute", "Useful"], "🔑", "Small, cheerful and easy to carry."),
    ("Notebook Set", (3, 9), ["Coworker", "Friend", "Woman", "Child"], ["Office", "Useful", "Cute"], "📓", "Handy for notes, plans and doodles."),
    ("Pen Set", (3, 8), ["Coworker", "Man"], ["Office", "Useful"], "🖊️", "A tidy set of smooth-writing pens."),
    ("Towel Gift Set", (5, 12), ["Family", "Woman", "Anyone"], ["Useful"], "🧺", "Soft towels, neatly gift-wrapped."),
    ("Hand Cream Set", (4, 12), ["Woman", "Friend", "Coworker"], ["Useful", "Cute"], "🧴", "A cosy little set for dry winter hands."),
    ("Small Tumbler", (6, 15), ["Anyone"], ["Useful"], "🥤", "A reusable cup for coffee, tea or water."),
    ("Phone Stand", (3, 8), ["Coworker", "Man", "Friend"], ["Tech", "Useful", "Office"], "📱", "Keeps a phone upright on any desk."),
    ("Cable Organizer", (2, 6), ["Coworker", "Man"], ["Tech", "Useful", "Office"], "🔌", "Ends the cable tangle for good."),
    ("Mini Artificial Plant", (4, 10), ["Anyone", "Coworker"], ["Cute", "Office"], "🪴", "Greenery that never needs watering."),
    ("Desk Organizer", (5, 14), ["Coworker"], ["Office", "Useful"], "🗄️", "Keeps pens and small items in order."),
    ("Reusable Shopping Bag", (2, 6), ["Anyone", "Family"], ["Useful"], "🛍️", "Light, foldable and eco-friendly."),
    ("Tea Gift Set", (5, 14), ["Family", "Friend", "Woman", "Man"], ["Food", "Useful"], "🍵", "A calm break in a box."),
    ("Coffee Pack", (5, 13), ["Coworker", "Man", "Friend"], ["Food", "Useful"], "☕", "Handy drip bags or instant sticks for coffee lovers."),
    ("Snack Box", (4, 12), ["Anyone", "Child", "Friend", "Family"], ["Food", "Funny"], "🍿", "An assortment of tasty snacks to share."),
    ("Small Plush Toy", (4, 12), ["Child", "Woman", "Friend"], ["Cute", "Funny"], "🧸", "Soft, huggable and always welcome."),
    ("Christmas Ornament", (2, 7), ["Anyone", "Family"], ["Christmas", "Cute"], "🎄", "A keepsake for the Christmas tree."),
    ("Coaster Set", (3, 8), ["Anyone", "Coworker", "Family"], ["Useful", "Office"], "🍽️", "Protects tables from mug rings."),
    ("Reusable Bottle", (5, 14), ["Anyone", "Coworker", "Man"], ["Useful"], "💧", "Stay hydrated all year long."),
    ("Stationery Set", (3, 9), ["Child", "Coworker", "Woman"], ["Office", "Cute", "Useful"], "✏️", "Pencils, erasers and notes in one pack."),
    ("Mini Desk Calendar", (3, 8), ["Coworker", "Anyone"], ["Office", "Useful", "Christmas"], "📅", "A cheerful way to plan the new year."),
    ("Small Storage Box", (4, 10), ["Family", "Woman", "Anyone"], ["Useful"], "📦", "Neat storage for small treasures."),
    ("Funny Novelty Mug", (4, 9), ["Friend", "Coworker", "Man"], ["Funny", "Useful"], "😂", "Guaranteed to get a laugh at the exchange."),
    ("Santa Hat", (2, 6), ["Anyone", "Child"], ["Christmas", "Funny", "Cute"], "🎅", "Instant holiday spirit for the party."),
    ("Wireless Earbuds (Basic)", (10, 25), ["Man", "Friend"], ["Tech"], "🎧", "Entry-level earbuds for music and calls."),
    ("Mini Power Bank", (8, 18), ["Coworker", "Man", "Friend"], ["Tech", "Useful"], "🔋", "Emergency phone charge in a pocket."),
    ("USB Flash Drive", (5, 10), ["Coworker"], ["Tech", "Office"], "💾", "Handy storage for documents and photos."),
    ("Gingerbread Cookie Tin", (4, 10), ["Anyone", "Child", "Family"], ["Food", "Christmas"], "🍪", "Festive cookies in a reusable tin."),
    ("Hot Cocoa Mix Set", (5, 12), ["Anyone", "Child", "Family"], ["Food", "Christmas"], "🎁", "Everything for a cosy cup of cocoa."),
    ("Card Game", (4, 12), ["Friend", "Family", "Child"], ["Funny", "Cute"], "🃏", "Quick, silly fun for any gathering."),
    ("Fairy Lights", (4, 10), ["Anyone"], ["Christmas", "Cute", "Useful"], "✨", "Warm string lights for a cosy corner."),
    ("Mouse Pad", (3, 8), ["Coworker", "Man"], ["Tech", "Office"], "🖱️", "A smooth, tidy surface for the mouse."),
]


def nice(v, mode):
    if mode == "unit":
        return max(1, round(v))
    if mode == "krw":
        return max(500, int(round(v / 500.0)) * 500)
    if mode == "php":
        return max(30, int(round(v / 10.0)) * 10)
    if mode == "jpy":
        step = 50 if v < 1000 else 100
        return max(100, int(round(v / step)) * step)
    if mode == "idr":
        step = 1000 if v < 20000 else 5000
        return max(5000, int(round(v / step)) * step)
    if mode == "ten":
        return max(10, int(round(v / 10.0)) * 10)
    if mode == "five":
        return max(10, int(round(v / 5.0)) * 5)
    raise ValueError(mode)


def step_of(mode):
    return {"unit": 1, "krw": 500, "php": 10, "jpy": 50, "idr": 1000, "ten": 10, "five": 5}[mode]


def main():
    gifts = []
    for country, code, symbol, factor, mode in COUNTRIES:
        rnd = random.Random(country)
        chosen = sorted(rnd.sample(range(len(T)), 26))
        for i in chosen:
            name, (lo, hi), rec, cats, icon, desc = T[i]
            j = rnd.uniform(0.9, 1.1)
            mn = nice(lo * factor * j, mode)
            mx = nice(hi * factor * rnd.uniform(0.92, 1.08), mode)
            if mx <= mn:
                mx = mn + step_of(mode)
            gifts.append({
                "country": country, "currencyCode": code, "currencySymbol": symbol,
                "giftName": name, "minPrice": mn, "maxPrice": mx,
                "recipients": rec, "categories": cats, "description": desc, "icon": icon,
            })
    out = os.path.join(os.path.dirname(__file__), "..", "finder", "src", "main", "assets", "gifts.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(gifts, f, ensure_ascii=True, indent=1)
    print(len(gifts), "gifts written to", os.path.normpath(out))


if __name__ == "__main__":
    main()
