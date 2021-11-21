from matplotlib import pyplot as plt
import csv
from absl import app, flags

FLAGS = flags.FLAGS

flags.DEFINE_string('name', None, "File name")

def main(argv):
    if FLAGS.name is None:
        print("Need file name. Use --name flag")
        return
    
    data = []
    _min = 10000
    _max = -10000

    with open(FLAGS.name, newline='') as f:
        rows = csv.reader(f)
        for row in rows:
            for d in row:
                _min = min(_min, float(d))
                _max = max(_max, float(d))
            data.append(row)

    for i in range(len(data[0])):
        plt.plot([float(row[i]) for row in data], 'o', markersize=2, label=str(i))
    plt.ylim([_min - 1, _max + 1])
    plt.ylabel('y-coordinate(m)')
    plt.xlabel('frame')
    plt.legend()
    plt.show()


if __name__  == '__main__':
    app.run(main)
