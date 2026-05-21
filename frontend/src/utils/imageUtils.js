const MAX_AVATAR_BYTES = 512 * 1024;
const readAvatarFile = (file) => {
    if (!file.type.startsWith("image/")) {
        return Promise.reject(new Error("Choose an image file."));
    }
    if (file.size > MAX_AVATAR_BYTES) {
        return Promise.reject(new Error("Choose an image under 512 KB."));
    }
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(new Error("Could not read image."));
        reader.readAsDataURL(file);
    });
};
export {
    readAvatarFile
};
