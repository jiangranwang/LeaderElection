confidence = 0.9
error = 0.02
p = 0.1

z = confidence + (1 - confidence) / 2
s = p * (1 - p) * (z / error) ** 2
print(s)