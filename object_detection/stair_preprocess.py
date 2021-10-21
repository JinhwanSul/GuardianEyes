import cv2
import os
from posix import listdir

def change_txt_format(txt_name, txt_path, target_path):
    with open(txt_path + txt_name, "r") as fd:
        text = fd.readline()
        raw_box = text.split(" ")
        print(raw_box, type(raw_box))
    
    center = (float(raw_box[1]), float(raw_box[2]))
    wh = (float(raw_box[3])/2, float(raw_box[4])/2)
    up_left = (center[0] - wh[0], center[1] - wh[1])
    bottom_right = (center[0] + wh[0], center[1] + wh[1])
    return up_left, bottom_right

def img_resize_and_save(img_name, img_path, target_path):
    raw_img = cv2.imread(img_path+img_name)
    if raw_img.shape[0] > 500 or raw_img.shape[1] > 500:
        target_img = cv2.resize(raw_img, dsize=(0, 0), fx = 0.25, fy = 0.25, interpolation = cv2.INTER_CUBIC)
        print(target_img.shape)
        cv2.imwrite(target_path+img_name, target_img)
        # cv2.imshow("resized",target_img)
    else:
        cv2.imwrite(target_path+img_name, raw_img)
        return

def draw_box(img, up_left, bottom_right):
    stair_image = cv2.imread(img)
    red_color = (0,0,255)

    height, width, _ = stair_image.shape
    up_left = (int(up_left[0]*width), int(up_left[1]*height))
    bottom_right = (int(bottom_right[0]*width), int(bottom_right[1]*height))
    stair_image = cv2.rectangle(stair_image, up_left, bottom_right, red_color, 4)
    cv2.imshow('Debug box', stair_image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

def main():
    test_img_path = "stairs_dataset/test/"
    train_img_path = "stairs_dataset/train/"
    target_path = "stairs_ready/"
    colab_path = "/content/drive/MyDrive/Colab Notebooks/TFLite_object_detection/stairs_ready/"
    stair_csv = "stair.csv"
    
    test_imgs = [x for x in os.listdir(test_img_path) if (x.endswith(".jpg") and (not x.endswith("_flipped.jpg")))]
    train_imgs = [x for x in os.listdir(train_img_path) if (x.endswith(".jpg") and (not x.endswith("_flipped.jpg")))]
    print("Test images: ", len(test_imgs))
    print("Train images: ", len(train_imgs))
    test_txts = [x for x in os.listdir(test_img_path) if (x.endswith(".txt") and (not x.endswith("_flipped.txt")))]
    train_txts = [x for x in os.listdir(train_img_path) if (x.endswith(".txt") and (not x.endswith("_flipped.txt")))]
    
    for i, train_img in enumerate(train_imgs):
        if i > 150:
            break
        train_txt = train_img.split(".")[0]+".txt"
        if train_txt not in train_txts:
            continue
        img_resize_and_save(train_img, train_img_path, target_path)
        up_left, bottom_right = change_txt_format(train_txt, train_img_path, target_path)
        print(up_left, bottom_right)
        # draw_box(target_path+train_img, up_left, bottom_right)
        
        fd = open(stair_csv, "a")
        line = "TRAIN,"+colab_path+train_img+",stair,"+str(up_left[0])+","+str(up_left[1])+",,,"+str(bottom_right[0])+","+str(bottom_right[1])+",,\n"
        fd.write(line)
        fd.close()
    
    for i, test_img in enumerate(test_imgs):
        if i > 50:
            break
        test_txt = test_img.split(".")[0]+".txt"
        if test_txt not in test_txts:
            continue
        img_resize_and_save(test_img, test_img_path, target_path)
        up_left, bottom_right = change_txt_format(test_txt, test_img_path, target_path)
        print(up_left, bottom_right)
        # draw_box(target_path+test_img, up_left, bottom_right)
        
        fd = open(stair_csv, "a")
        if i%2 == 0:
            line = "TEST,"+colab_path+test_img+",stair,"+str(up_left[0])+","+str(up_left[1])+",,,"+str(bottom_right[0])+","+str(bottom_right[1])+",,\n"
        else:
            line = "VALIDATE,"+colab_path+test_img+",stair,"+str(up_left[0])+","+str(up_left[1])+",,,"+str(bottom_right[0])+","+str(bottom_right[1])+",,\n"
        fd.write(line)
        fd.close()

if __name__ == "__main__":
    main()