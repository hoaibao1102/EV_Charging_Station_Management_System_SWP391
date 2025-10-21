import styled from 'styled-components';

const Card = styled.div`
  border: 1px solid #20b2aa;
  background-color: #f9f9f9;
  border-radius: 10px;
  padding: 16px;
  margin: 8px 0;
  ;
`;

const Title = styled.h5`
  margin: 0 0 8px;
  color: #20b2aa;
`;

const Content = styled.p`
  margin: 0 0 8px;
`;

const Date = styled.i`
  font-size: 0.8em;
  color: #666;
`;

export default function NotificationCard({ notification }) {

  return (
    <Card>
      <Title>📰{notification.title}📢</Title>
      <Content>{notification.content}</Content>
      <Date>{notification.createdAt}</Date>
    </Card>
  )
}


