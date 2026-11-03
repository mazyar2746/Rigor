import urllib.request
import json
import time
import os

# خواندن لیست ۷۸۸ نمادی
with open("symbols_list.json", "r", encoding="utf-8") as f:
    symbols = json.load(f)

print(f"تعداد نمادها: {len(symbols)}")
print("در حال دریافت دیتای قیمتی (این ممکنه ۱۰-۱۵ دقیقه طول بکشه)...")

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
all_data = {}
failed = []

for i, item in enumerate(symbols):
    name = item["name"]
    code = item["insCode"]
    
    if i % 50 == 0:
        print(f"  [{i}/{len(symbols)}] {name}...")
    
    url = f"https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/{code}/0"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            data = json.loads(res.read().decode('utf-8'))
            if "closingPriceDaily" in data and len(data["closingPriceDaily"]) > 0:
                all_data[name] = {
                    "insCode": code,
                    "prices": data["closingPriceDaily"][:30]  # ۳۰ روز آخر
                }
    except Exception as e:
        failed.append(name)
    time.sleep(0.3)  # فاصله برای جلوگیری از بلاک شدن

# ذخیره
with open("market_data_full.json", "w", encoding="utf-8") as f:
    json.dump(all_data, f, ensure_ascii=False, indent=2)

print(f"\n✅ {len(all_data)} نماد دریافت شد.")
print(f"❌ {len(failed)} نماد ناموفق: {failed[:10]}...")
