import styled from 'styled-components';
import classed from '../../assets/css/Main.module.css';


const Card = styled.div`
  border: 1px solid #20b2aa;
  background-color: #f9f9f9;
  border-radius: 10px;
  padding: 16px;
  margin: 8px 0;
  display: flex;
  flex-direction: column;
  min-height: 150px; /* Tùy chỉnh chiều cao tối thiểu nếu cần */
`;

const Title = styled.h5`
  margin: 0 0 8px;
  color: #20b2aa;
`;

const Content = styled.p`
  margin: 0 0 8px;
  flex: 1; /* Cho phép content chiếm hết không gian còn lại */
`;

const Date = styled.i`
  margin-top: auto; /* Đẩy ngày xuống dưới cùng */
  padding-top: 8px;
  font-size: 0.8em;
  color: #666;
`;

export default function NotificationCard({ notification, onSelect }) {
  
  const handleMarkAsRead = () => {
    if (onSelect) {
      onSelect(notification);
    }
  };

  return (
    <Card>
      <Title>📰{notification.title}📢</Title>
      <Content>{notification.content}</Content>
      <Date>{notification.createdAt}</Date>
      {notification.status === 'UNREAD' && (
        <button className={classed.button} onClick={handleMarkAsRead}>
          Đã đọc ✅
        </button>
      )}
    </Card>
  )
}