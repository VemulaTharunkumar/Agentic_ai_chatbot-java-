import React, { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { LogOut, Copy, Send, Bot, Check, LayoutDashboard, Brain, Globe, Code, FileText, Trash2 } from 'lucide-react';
import ThinkingLoader from './ThinkingLoader';
import './Dashboard.css';

const API_URL = "https://agentic-ai-chatbot-1-30s7.onrender.com";

const Dashboard = ({ user, onLogout }) => {

  const [task, setTask] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [expandedSection, setExpandedSection] = useState(false);
  const [chatHistory, setChatHistory] = useState([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(true);
  const [selectedHistoryItem, setSelectedHistoryItem] = useState(null);
  const [popupAgent, setPopupAgent] = useState(null);

  /* ---------------- HISTORY FETCH ---------------- */

  useEffect(() => {

    const fetchHistory = async () => {
      if (!user?.username) return;

      try {
        const res = await fetch(`${API_URL}/api/history/${user.username}`);

        if (res.ok) {
          const data = await res.json();
          setChatHistory(data.history.reverse());
        }

      } catch (err) {
        console.error("Failed to load history", err);
      } finally {
        setIsLoadingHistory(false);
      }
    };

    fetchHistory();

  }, [user]);


  /* ---------------- TASK SUBMIT ---------------- */

  const handleSubmit = async (e) => {

    e.preventDefault();
    if (!task.trim()) return;

    setIsProcessing(true);
    setResult(null);
    setError('');
    setExpandedSection(false);
    setSelectedHistoryItem(null);
    setPopupAgent(null);

    try {

      const response = await fetch(`${API_URL}/api/task`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task, username: user.username })
      });

      const data = await response.json();

      if (response.ok) {

        setResult(data);

        const newHistoryItem = {
          _id: data.chat_id || Date.now().toString(),
          prompt: task,
          response: data.final_answer,
          agent: data.agents.join(", "),
          timestamp: new Date().toISOString()
        };

        setChatHistory(prev => [...prev, newHistoryItem]);

      } else {
        setError(data.detail || "Task failed");
      }

    } catch (err) {
      setError("Could not connect to backend server");
    } finally {
      setIsProcessing(false);
    }
  };


  /* ---------------- DELETE CHAT ---------------- */

  const handleDeleteChat = async (e, chatId) => {

    e.stopPropagation();

    if (!window.confirm("Delete this chat history?")) return;

    try {

      const res = await fetch(`${API_URL}/api/history/${chatId}`, {
        method: 'DELETE'
      });

      if (res.ok) {

        setChatHistory(prev => prev.filter(c => c._id !== chatId));

        if (selectedHistoryItem?._id === chatId) {
          setSelectedHistoryItem(null);
        }

      }

    } catch (err) {
      console.error("Delete failed", err);
    }
  };


  /* ---------------- COPY CODE ---------------- */

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };


  /* ---------------- AGENT ICONS ---------------- */

  const renderAgentIcon = (agent) => {

    switch (agent) {

      case "planner":
        return <Brain size={18} />;

      case "research":
        return <Globe size={18} />;

      case "coder":
        return <Code size={18} />;

      case "critic":
        return <FileText size={18} />;

      default:
        return <Bot size={18} />;
    }
  };


  return (

    <div className="dashboard-layout">

      {/* Sidebar */}

      <aside className="sidebar">

        <div className="sidebar-header">
          <h2>🤖 Agentic AI</h2>
        </div>

        <div className="sidebar-history">

          <h3>Chat History</h3>

          {isLoadingHistory ? (

            <p>Loading...</p>

          ) : chatHistory.length > 0 ? (

            chatHistory.map(chat => (

              <div
                key={chat._id}
                className="history-list-item"
                onClick={() => setSelectedHistoryItem(chat)}
              >

                <Bot size={14} />

                <span>{chat.prompt}</span>

                <button
                  onClick={(e) => handleDeleteChat(e, chat._id)}
                >
                  <Trash2 size={14} />
                </button>

              </div>

            ))

          ) : (

            <p>No chat history</p>

          )}

        </div>

        <div className="user-profile">

          <span>{user?.username}</span>

          <button onClick={onLogout}>
            <LogOut size={18} />
          </button>

        </div>

      </aside>


      {/* Main */}

      <main className="main-content">

        <h1>Welcome {user?.username}</h1>

        <form onSubmit={handleSubmit}>

          <textarea
            placeholder="Ask anything..."
            value={task}
            onChange={(e) => setTask(e.target.value)}
          />

          <button type="submit" disabled={isProcessing}>
            {isProcessing ? "Thinking..." : "Run Task"}
            <Send size={16}/>
          </button>

        </form>


        {error && <p>{error}</p>}

        {isProcessing && <ThinkingLoader/>}


        {result && (

          <div>

            <h3>Final Answer</h3>

            {result.is_code ? (

              <pre>
                <code>{result.final_answer}</code>
              </pre>

            ) : (

              <ReactMarkdown>{result.final_answer}</ReactMarkdown>

            )}

          </div>

        )}

      </main>

    </div>

  );
};

export default Dashboard;