import csv

input_file = "/tmp/posts_all.csv"
output_file = "/opt/app/iCampus_rec/data/posts_train.csv"

with open(input_file, 'r', encoding='utf-8') as infile, \
     open(output_file, 'w', encoding='utf-8', newline='') as outfile:
    reader = csv.reader(infile)
    writer = csv.writer(outfile, quoting=csv.QUOTE_ALL)  # 所有字段都加引号
    for row in reader:
        writer.writerow(row)

print(f"Fixed CSV saved to {output_file}")
