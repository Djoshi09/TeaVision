# Agar tensorflow install nahi hai, toh pehle yeh run karein:
# !pip install tensorflow

import os
from tensorflow.keras.preprocessing.image import (
    ImageDataGenerator,
    img_to_array,
    load_img,
    save_img,
)
import numpy as np

# Yahan apne main folder ka path daalein
dataset_folder = r"C:\Users\deept\Fpxnet Dropbox\xafolo xafolo\shareit\course\Deepu\tea project\advanced images"
target_images_per_class = 300  # Humein har folder mein 300 photos chahiye

# Data Augmentation ki settings (AI ko nayi photos sikhane ke liye)
datagen = ImageDataGenerator(
    rotation_range=30,  # Photo ko 30 degree tak ghumayega
    width_shift_range=0.1,  # Photo ko thoda left-right shift karega
    height_shift_range=0.1,  # Photo ko thoda up-down shift karega
    zoom_range=0.2,  # 20% tak zoom-in ya zoom-out karega
    horizontal_flip=True,  # Photo ko mirror (palat) dega
    brightness_range=[
        0.8,
        1.2,
    ],  # Brightness kam ya zyada karega (taaki alag lighting cover ho)
    fill_mode="nearest",  # Khali jagah ko aas-pas ke colors se bhar dega
)

# Har tea ke folder mein jaakar check karega
for tea_name in os.listdir(dataset_folder):
    # Skip hidden and system folders
    if tea_name.startswith("."):
        continue

    tea_folder_path = os.path.join(dataset_folder, tea_name)

    # Agar wo folder nahi hai (kisi file ko skip karne ke liye)
    if not os.path.isdir(tea_folder_path):
        continue

    # Ensure folder exists
    os.makedirs(tea_folder_path, exist_ok=True)

    # Current folder mein sirf original photos count karein (augmented nahi)
    images = [
        img
        for img in os.listdir(tea_folder_path)
        if img.endswith(("jpg", "jpeg", "png")) and not img.startswith("aug")
    ]
    current_count = len(images)

    print(f"[{tea_name}] Original photos: {current_count}")

    # Agar 300 se kam hain, tabhi augment karega
    if current_count < target_images_per_class:
        # Skip if no original images to augment from
        if current_count == 0:
            print(
                f"⚠️ [{tea_name}] mein koi original photo nahi hai, augmentation skip kar rahe hain.\n"
            )
            continue

        images_needed = target_images_per_class - current_count
        print(f"[{tea_name}] Generating {images_needed} new augmented photos...")

        # Ek loop chalayenge jab tak target poora na ho jaye
        generated_count = 0
        aug_counter = 0  # Counter for unique augmented image names

        while generated_count < images_needed:
            for image_name in images:
                if generated_count >= images_needed:
                    break

                img_path = os.path.join(tea_folder_path, image_name)

                try:
                    # Photo ko read karein
                    img = load_img(img_path)
                    x = img_to_array(img)
                    x = x.reshape((1,) + x.shape)

                    # Nayi photo banayein aur usi folder mein save karein (reliable tarika)
                    augmented_batch = datagen.flow(x, batch_size=1)
                    augmented_image = next(augmented_batch)[0].astype(np.uint8)

                    aug_img_path = os.path.join(
                        tea_folder_path, f"aug_{aug_counter:05d}.jpg"
                    )
                    save_img(aug_img_path, augmented_image)
                    aug_counter += 1
                    generated_count += 1

                except Exception as e:
                    print(f"Error in {image_name}: {e}")

        print(f"✅ [{tea_name}] ab {target_images_per_class} photos ho gayi hain!\n")
    else:
        print(f"✅ [{tea_name}] mein pehle se hi kaafi photos hain.\n")

print("Saari teas ki Data Augmentation complete ho gayi!")
