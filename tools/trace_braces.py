with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'r') as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    depth += line.count('{') - line.count('}')
    if depth == 1 and line.count('}') > 0:
        print(f"Depth 1 closing at {i+1}: {line.strip()}")
