import React from "react";
import {getPoliceListApi} from "../../api/policeApi.js";
import {useState, useEffect} from "react";

export default function Rules() {
  const styles = {
    container: { 
      padding: 32,
      paddingBottom: 120, 
      maxWidth: 1000,
      margin: "0 auto",
      backgroundColor: "#ffffff",
      minHeight: "100vh"
    },
    header: {
      borderBottom: "3px solid #20b2aa",
      paddingBottom: 16,
      marginBottom: 24
    },
    heading: { 
      fontSize: "2rem",
      fontWeight: 700,
      marginBottom: 8,
      color: "#1a1a1a"
    },
    tableWrap: { 
      overflowX: "auto",
      overflowY: "auto",
      maxHeight: "58vh",
      backgroundColor: "#ffffff",
      borderRadius: 8,
      boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
      // Custom scrollbar
      scrollbarWidth: "thin",
      scrollbarColor: "#20b2aa #f0f0f0"
    },
    table: { 
      width: "100%",
      borderCollapse: "collapse"
    },
    cell: { 
      border: "1px solid #20b2aa",
      padding: 16,
      verticalAlign: "top",
      backgroundColor: "#ffffff"
    },
    th: { 
      background: "#20b2aa",
      fontWeight: 600,
      color: "#ffffff",
      textAlign: "left"
    },
    codeCell: {
      fontWeight: 700,
      color: "#20b2aa",
      fontSize: "0.95rem"
    },
    note: { 
      color: "#666666",
      marginTop: 24,
      fontSize: 14,
      padding: 16,
      backgroundColor: "#f0fffe",
      borderLeft: "4px solid #20b2aa",
      borderRadius: 4
    },
  };

  const [rules, setRules] = useState([]);
  useEffect(() => {
    const fetchRules = async () => {
      try {
        const data = await getPoliceListApi();
        setRules(data.data);
        console.log("Fetched rules:", data);
      } catch (error) {
        console.error("Failed to fetch rules:", error);
      }
    };

    fetchRules();
  }, []);


  return (
    <div style={styles.container}>
      <style>{`
        /* Custom scrollbar cho bảng quy định */
        .rules-table-wrap::-webkit-scrollbar {
          width: 10px;
          height: 10px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-track {
          background: #f0f0f0;
          border-radius: 5px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-thumb {
          background: #20b2aa;
          border-radius: 5px;
        }
        
        .rules-table-wrap::-webkit-scrollbar-thumb:hover {
          background: #1a9e98;
        }
      `}</style>

      <div style={styles.header}>
        <h1 style={styles.heading}>Quy định sử dụng hệ thống sạc EV</h1>
      </div>

      <div style={styles.tableWrap} className="rules-table-wrap">
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={{ ...styles.cell, ...styles.th, width: 120 }}>Mã quy định</th>
              <th style={{ ...styles.cell, ...styles.th }}>Quy định</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.policyId}>
                <td style={{ ...styles.cell, ...styles.codeCell }}>
                  {r.policyName}
                </td>
                <td style={styles.cell}>{r.policyDescription}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={styles.note}>
        <strong>Lưu ý:</strong> Mức phí phạt và thời lượng mỗi slot có thể khác nhau theo loại cổng sạc và cấu hình của từng trạm.
      </div>
    </div>
  );
}