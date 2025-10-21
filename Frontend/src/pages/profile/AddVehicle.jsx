import { getAllVehicleBrandsApi, getModelsByBrandApi } from '../../api/driverApi.js';
import { useEffect, useState } from 'react';
import { addVehicleApi } from '../../api/driverApi.js';
// import {toast} from 'react-toastify';
import ModelVehicle from '../../components/ModelVehicle.jsx';
import './AddVehicle.css';
// 🚗 Import các icons
import { FaCar, FaChevronLeft, FaPlus, FaTachometerAlt, FaWarehouse, FaCheckCircle, FaExclamationTriangle } from 'react-icons/fa';

export default function AddVehicle({ onClose, onSuccess }) {
    const [step, setStep] = useState(1);
    const [brands, setBrands] = useState([]);
    const [models, setModels] = useState([]);
    const [brand, setBrand] = useState('');

    const [vehicle, setVehicle] = useState({
        modelId: '',
        licensePlate: '',
    });


    const handleAddVehicle = async () => {
        if (!vehicle.licensePlate || !vehicle.modelId) {
            // ⚠️ Icon cho cảnh báo
            alert(`⚠️ Vui lòng nhập đầy đủ thông tin xe!`);
            return;
        } else {
            try {
                console.log('Adding vehicle:', vehicle);
                const response = await addVehicleApi(vehicle);
                if (response.success) {
                    // ✅ Icon cho thành công
                    alert(`✅ Thêm xe thành công!`);
                    setVehicle({ modelId: '', licensePlate: '' });
                    setBrand('');
                    setStep(1);
                    if (onSuccess) {
                        onSuccess();
                    }
                    if (onClose) {
                        onClose();
                    }
                } else {
                    alert(`❌ ${response.message || 'Thêm xe thất bại!'}`);
                }
            } catch (error) {
                console.error('Failed to add vehicle:', error);
                alert(`🛑 Lỗi hệ thống: ${error.response?.data || error.message}`);
            }
        }
    };
    useEffect(() => {
        const fetchBrands = async () => {
            const response = await getAllVehicleBrandsApi();
            if (response.success) {
                setBrands(response.data);
            } else {
                console.error('Failed to fetch vehicle brands:', response.message);
            }
        };

        fetchBrands();
    }, []);

    
    useEffect(() => {
        if (!brand) return;
        const fetchModelsByBrand = async (brand) => {
            const response = await getModelsByBrandApi(brand);
            if (response.success) {
                console.log('Models for brand', brand, ':', response.data);
                setModels(response.data);
            } else {

                console.error('Failed to fetch vehicle models:', response.message);
            } 
        };

        fetchModelsByBrand(brand);
    }, [brand]);

    

    return (
        <div className="add-vehicle-container" >
            {/* //STEP 1: CHỌN BRAND XE CÓ TRONG HỆ THỐNG và model xe tương ứng */}
            {step === 1 && (
            <div> 
                <h2><FaCar style={{ marginRight: '8px' }} /> Chọn thương hiệu xe</h2>
                <select
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                >
                    <option value=""><FaWarehouse /> Chọn thương hiệu</option>
                    {brands.map((brand) => (
                        <option key={brand} value={brand}>
                            {brand}
                        </option>
                    ))}
                </select>
                {models.length > 0 && (
                    <div> 
                        <h2><FaTachometerAlt style={{ marginRight: '8px' }} /> Chọn mẫu xe</h2>
                        {models.map((model) => (
                            <ul key={model.id} style={{listStyleType: 'none', padding: 0, cursor: 'pointer'}} 
                                onClick={() => {setVehicle({ ...vehicle, modelId: model.modelId}); setStep(2);}}>
                                <li>
                                    <ModelVehicle model={model} />
                                </li>
                            </ul>
                            
                        ))}
                    </div>
                )}
            </div>
            )}
            {/* //STEP 2: NHẬP THÔNG TIN XE */}
            {step === 2 && ( 
                <div>
                    <button onClick={() => setStep(1)}><FaChevronLeft /> Quay lại</button>
                    <h2><FaCar style={{ marginRight: '8px' }} /> Nhập thông tin xe</h2>
                    <label>
                        Biển số xe:
                        <input
                            type="text"
                            value={vehicle.licensePlate}
                            onChange={(e) => setVehicle({ ...vehicle, licensePlate: e.target.value })}
                            placeholder="Ví dụ: 51A12345"
                        />
                    </label>
                    <br />
                    <button
                        onClick={handleAddVehicle}
                    >
                        <FaPlus style={{ marginRight: '5px' }} /> Thêm xe
                    </button>
                </div>
            )}
        </div>
    );
}