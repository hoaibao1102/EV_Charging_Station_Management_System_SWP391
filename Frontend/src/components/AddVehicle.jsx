import {getAllVehicleBrandsApi, getModelsByBrandApi} from '../api/driverApi.js';
import { useEffect, useState } from 'react';

export default function AddVehicle() {
    const [step, setStep] = useState(1);
    const [brands, setBrands] = useState([]);
    const [models, setModels] = useState([]);
    const [brand, setBrand] = useState('');
    // const [vehicle, setVehicle] = useState({
    // modelId: '',
    // licensePlate: '',
    // });

    const [model, setModel] = useState({
      modelId: '',
      brand: '',
      model: '',
      year: '',
      connectorTypeId: '',
      connectorTypeCode: '',
      connectorTypeDisplayName: '',
      connectorDefaultMaxPowerKW: '',   
    });

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
               setModels(response.data);
           } else {
               console.error('Failed to fetch vehicle models:', response.message);
           } 
        };

        fetchModelsByBrand();
    }, [brand]);

    

  return (
    <>
       {/* //STEP 1: CHỌN BRAND XE CÓ TRONG HỆ THỐNG và model xe tương ứng */}
       {step === 1 && (
        <div>   
          <h2>Chọn thương hiệu xe</h2>
          <select
            value={brand}
            onChange={(e) => setBrand(e.target.value)}
          >
            <option value="">Chọn thương hiệu</option>
            {brands.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
            {models.length > 0 && (
              <div>
                <h2>Chọn mẫu xe</h2>
                <select
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                >
                  <option value="">Chọn mẫu xe</option>
                  {models.map((model) => (
                    <option key={model.id} value={model.id}>
                      {model.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
        </div>
      )}
    </>
  )
}