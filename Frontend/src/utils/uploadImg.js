import { toast } from 'react-toastify';
import {getUploadSignature} from '../api/uploadApi.js';
import axios from 'axios';

export const uploadImageToCloudinary = async (file) => {
  if (!file) {
    toast.error('Đang không có file nào được chọn.');
    throw new Error('No file selected for upload.');
  }

  try {
    const sigData = await getUploadSignature();

    // --- BƯỚC 2: Dùng "vé" upload thẳng lên Cloudinary ---
    if(sigData.success){
        console.log('lấy đc data ne: ', sigData.data);
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('api_key', sigData.data.api_key);
    formData.append('timestamp', sigData.data.timestamp);
    formData.append('signature', sigData.data.signature);
    
    // Thêm folder nếu bạn có cấu hình (ví dụ: "vehicle_models")
    if (sigData.data.folder) {
      formData.append('folder', sigData.data.folder);
    }

    const uploadUrl = `https://api.cloudinary.com/v1_1/${sigData.data.cloud_name}/image/upload`;

    // Dùng 'axios' gốc (không cần token) để gọi Cloudinary
    const uploadResponse = await axios.post(uploadUrl, formData);

    // --- BƯỚC 3: Trả về kết quả ---
    // uploadResponse.data sẽ là: { public_id: "...", secure_url: "http://..." }
    return uploadResponse; 

  } catch (error) {
    console.error('Error during image upload process:', error);
    if (error.response && error.response.status === 401) {
        throw new Error('Unauthorized. Could not get upload signature. Please log in.');
    }
    throw new Error('Image upload failed.');
  }
};