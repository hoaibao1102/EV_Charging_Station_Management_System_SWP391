import './index.css';
import { createRoot } from 'react-dom/client';
import AppRouter from './path/AppRouter.jsx'
import { store } from './redux/store.js';
import { Provider } from 'react-redux';
import 'bootstrap/dist/css/bootstrap.min.css';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

createRoot(document.getElementById('root')).render(
        <>
        <Provider store={store}>
                <AppRouter />
                <ToastContainer position="top-center" autoClose={2000} />
        </Provider>
        </>
);
