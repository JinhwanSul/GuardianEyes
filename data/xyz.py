from matplotlib import pyplot as plt
import csv
from absl import app, flags
from scipy.signal import medfilt
import math

FLAGS = flags.FLAGS

flags.DEFINE_string('name', None, "File name")
flags.DEFINE_string('target', "camera", "target name")
flags.DEFINE_string('xyz', 'xz', 'coordinate')

class pos:
    x = []
    y = []
    z = []

def smoothing(t, num):
    res = []
    for k in range(len(t) - num + 1):
        avg = sum(t[k:k+num]) / num
        res.append(avg)
    
    return res

def diff(t, num):
    res = []
    for k in range(0, len(t) - num, num):
        dif = t[k + num] - t[k]
        res.append(dif)
    return res

def difference(x, y):
    res = []
    for a, b in zip(x, y):
        res.append(round(b - a))
    return res

def differenciate(x, y, z, num):
    res = []
    for k in range(0, len(x) - num):
        dx = x[k + num] - x[k]
        dy = y[k + num] - y[k]
        dz = z[k + num] - z[k]
        d = dy / math.sqrt(dx * dx + dz * dz)
        res.append(d)
    return res

def increasing(t, threshold = 0):
    res = 1
    for i in range(len(t) - 1):
        if t[i + 1] < t[i]:
            return -1
        if t[i + 1] > t[i] + threshold:
            res = res + 1
    return res

def trigger(t, threshold = 0):
    for i in range(len(t) - 1):
        if t[i + 1] <= t[i] + threshold:
            return False
    return True

# def monoincreasing(t):
#     for i in range(len(t) - 1):
#         if t[i + 1] < t[i]:
#             return False
#     return True

def main(argv):
    if FLAGS.name is None:
        print("Need file name. Use --name flag")
        return
    
    data = []
    # _min = 10000
    # _max = -10000

    with open(FLAGS.name, newline='') as f:
        rows = csv.reader(f)
        for row in rows:
            # for d in row[1:]:
            #     _min = min(_min, float(d))
            #     _max = max(_max, float(d))
            data.append(row)

    cameraPos = pos()
    targetPos = pos()
    cameraPos.x = [float(row[1]) * 100 for row in data]
    cameraPos.y = [float(row[2]) * 100 for row in data]
    cameraPos.z = [float(row[3]) * 100 for row in data]
    targetPos.x = [float(row[4]) * 100 for row in data]
    targetPos.y = [float(row[5]) * 100 for row in data]
    targetPos.z = [float(row[6]) * 100 for row in data]
    time = [float(row[0]) / 1000 for row in data]
    values = range(len(time))

    # for i in range(len(data[0])):
    #     plt.plot([float(row[i]) for row in data], 'o', markersize=2, label=str(i))
    # plt.ylim([_min - 1, _max + 1])
    # plt.ylabel('y-coordinate(m)')
    # plt.xlabel('frame')

    # target = cameraPos
    # if(FLAGS.target != "camera"):
    #     target = targetPos

    if(FLAGS.xyz != 'xz'):
        y = difference(cameraPos.y, targetPos.y)

        x = medfilt(cameraPos.x, 101)
        y = medfilt(y, 101)
        # plt.plot(y, '-o', color='r', markersize=3)

        z = medfilt(cameraPos.z, 101)

        y = differenciate(x, y, z, 50)
        y = medfilt(y, 51)

        # y = medfilt(y, 15)

        # plt.plot(y, '-o', color='r', markersize=3)
        # plt.plot(diff(sy, 15), '-o', color='r', markersize=3)
        # plt.plot(target.y, '-o', color='r', markersize=3)
    else:
        x = medfilt(cameraPos.x, 51)
        z = medfilt(cameraPos.z, 51)

        # y = difference(cameraPos.y, targetPos.y)
        # y = medfilt(y, 101)
        # y = targetPos.y
        # y = cameraPos.y
        print(len(cameraPos.y))
        cy = medfilt(cameraPos.y, 15)
        print(len(cy))
        ty = medfilt(targetPos.y, 15)
        # y = difference(cy, ty)
        ty_round = []
        for i in ty:
            ty_round.append(round(i))

        y = ty_round
        
        # y = list(medfilt(y, 15))

        kernel_size = 10
        # half = (int) (kernel_size / 2)
        for _ in range(kernel_size):
            y.insert(0, y[0])
            y.append(y[len(y) - 1])
        
        # threshold = 3
        res = []
        # for i in range(len(y) + kernel_size):
        #     front = sum(y[i:i+half]) / (kernel_size/2)
        #     back = sum(y[i+half:i+kernel_size]) / (kernel_size/2)

        #     if back - front < threshold:
        #         res.append(0)
        #     else:
        #         res.append(1)

        trigger_threshold = 3
        wall_threshold = kernel_size - (kernel_size / 4)
        up_threshold = kernel_size / 2
        tuck_threshold = 5
        for i in range(len(y) - kernel_size):
            if trigger(y[i:i+trigger_threshold]):
                inc = increasing(y[i:i+kernel_size])
                if inc > wall_threshold:
                    res.append(3)
                elif inc > up_threshold:
                    res.append(2)
                elif (y[i + kernel_size - 1] - y[i]) >= tuck_threshold:
                    res.append(1)
                else:
                    res.append(0)
            else:
                res.append(0)

        # plt.plot(res, '-o', color='r', markersize=3)
        plt.plot(y, '-o', color='r', markersize=3)

    # plt.legend()  
    # ax = plt.gca()
    # ax.set_xticks(time)
    plt.xticks(values, time)
    plt.xticks(rotation = 50)
    plt.locator_params(nbins=20)
    plt.show()


if __name__  == '__main__':
    app.run(main)



