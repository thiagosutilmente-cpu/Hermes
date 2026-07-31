with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("const ridePickup = (ride.pickup_address || `\").toLowerCase();", "const ridePickup = (ride.pickup_address || \"\").toLowerCase();")

with open('index.html', 'w') as f:
    f.write(content)
