import React, { useState } from 'react';
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

  React.useEffect(() => {
    const fetchHistory = async () => {
      if (!user?.username) return;
      try {
        const res = await fetch(`${API_URL}/api/history/${user.username}`);
        if (res.ok) {
          const data = await res.json();
          // The API returns it sorted newest first (timestamp DESC). We want oldest first for display flow.
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
      const response = await fetch('https://agentic-ai-chatbot-1-30s7.onrender.com/api/task', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task, username: user.username })
      });

      const data = await response.json();

      if (response.ok) {
        setResult(data);
        
        // Append to chat history
        const newHistoryItem = {
          _id: data.chat_id || Date.now().toString(), // Use actual DB ID
          prompt: task,
          response: data.final_answer,
          agent: data.agents.join(", "),
          timestamp: new Date().toISOString()
        };
        setChatHistory(prev => [...prev, newHistoryItem]);
        
      } else {
        setError(data.detail || 'An error occurred while processing the task.');
      }
    } catch (err) {
      setError('Could not connect to the backend server. Is it running?');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDeleteChat = async (e, chatId) => {
    e.stopPropagation(); // prevent selecting the chat when clicking delete

    if (!window.confirm("Are you sure you want to delete this chat history?")) return;

    try {
      const res = await fetch(`https://agentic-ai-chatbot-1-30s7.onrender.com/api/history/${chatId}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        // Remove from local state
        setChatHistory(prev => prev.filter(c => c._id !== chatId));
        
        // If the deleted items is currently selected, clear the view
        if (selectedHistoryItem?._id === chatId) {
          setSelectedHistoryItem(null);
        }
      } else {
        console.error("Failed to delete chat record");
      }
    } catch (err) {
      console.error("Could not reach backend to delete chat", err);
    }
  };

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const renderAgentIcon = (agent) => {
    switch (agent) {
      case 'planner': return <Brain className="agent-icon planner-icon" size={18} />;
      case 'research': return <Globe className="agent-icon research-icon" size={18} />;
      case 'coder': return <Code className="agent-icon coder-icon" size={18} />;
      case 'critic': return <FileText className="agent-icon critic-icon" size={18} />;
      default: return <Bot className="agent-icon" size={18} />;
    }
  };

  return (
    <div className="dashboard-layout">
      {/* Sidebar Area */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="app-branding">
            <span className="logo-emoji">🤖</span>
            <h2>Agentic AI</h2>
          </div>
        </div>
        
        <nav className="sidebar-nav">
          <div 
            className={`nav-item ${!selectedHistoryItem ? 'active' : ''}`}
            onClick={() => setSelectedHistoryItem(null)}
          >
            <LayoutDashboard size={20} />
            <span>New Task</span>
          </div>
        </nav>

        {/* Sidebar History List */}
        <div className="sidebar-history">
          <h3 className="history-title">Chat History</h3>
          {isLoadingHistory ? (
            <div className="history-loader-small">Loading...</div>
          ) : chatHistory.length > 0 ? (
            <div className="history-list">
              {chatHistory.map((chat) => (
                <div 
                  key={chat._id} 
                  className={`history-list-item ${selectedHistoryItem?._id === chat._id ? 'active' : ''}`}
                  onClick={() => setSelectedHistoryItem(chat)}
                >
                  <Bot size={14} className="history-icon" />
                  <span className="history-truncate">{chat.prompt}</span>
                  <button 
                    className="delete-chat-btn"
                    onClick={(e) => handleDeleteChat(e, chat._id)}
                    title="Delete Chat"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="history-empty">No past chats</div>
          )}
        </div>

        <div className="user-profile">
          <div className="avatar">
            {user?.username?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="user-info">
            <span className="username">{user?.username || 'User'}</span>
            <span className="status">Online</span>
          </div>
          <button className="logout-btn" onClick={onLogout} title="Logout">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content">
        <header className="content-header">
          <div className="header-greeting">
            <h1>Welcome back, {user?.username}! 👋</h1>
            <p>What would you like me to work on today?</p>
          </div>
        </header>

        <div className="task-container">
          
          {selectedHistoryItem ? (
            <div className="historical-view animate-fade-in">
              <button 
                className="back-btn glass-panel"
                onClick={() => setSelectedHistoryItem(null)}
              >
                ← Back to New Task
              </button>
              
              <div className="history-item">
                <div className="history-prompt glass-panel">
                  <div className="prompt-header">
                    <div className="avatar user-avatar-small">
                      {user?.username?.charAt(0).toUpperCase()}
                    </div>
                    <span className="timestamp">
                      {new Date(selectedHistoryItem.timestamp).toLocaleString()}
                    </span>
                  </div>
                  <div className="prompt-text">{selectedHistoryItem.prompt}</div>
                </div>
                
                <div className="history-response glass-panel">
                  <div className="response-header">
                    <div className="avatar bot-avatar-small">🤖</div>
                    <span className="agent-badge">{selectedHistoryItem.agent}</span>
                  </div>
                  <div className="response-content markdown-body">
                    <ReactMarkdown>{selectedHistoryItem.response}</ReactMarkdown>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <>
              <form onSubmit={handleSubmit} className="task-input-form glass-panel">
            <textarea
              className="task-textarea"
              placeholder="Ask anything you want to learn or build..."
              value={task}
              onChange={(e) => {
                setTask(e.target.value);
                // Auto-resize logic
                e.target.style.height = 'auto';
                e.target.style.height = `${e.target.scrollHeight}px`;
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSubmit(e);
                }
              }}
              disabled={isProcessing}
            />
            <div className="form-footer">
              <span className="hint">Press <strong>Enter</strong> to run, <strong>Shift + Enter</strong> for new line</span>
              <button 
                type="submit" 
                className={`run-button ${isProcessing ? 'processing' : ''}`}
                disabled={!task.trim() || isProcessing}
              >
                {isProcessing ? 'Thinking...' : 'Run Task'}
                {!isProcessing && <Send size={16} className="send-icon" />}
              </button>
            </div>
          </form>

          {/* Error Message */}
          {error && (
            <div className="error-message animate-fade-in">
              <p>{error}</p>
            </div>
          )}

          {/* Loader */}
          {isProcessing && <ThinkingLoader />}

          {/* Results Area */}
          {result && !isProcessing && (
            <div className="results-container animate-fade-in">
              
              {/* Used Agents Chips */}
              <div className="agents-used">
                <span className="agents-label">Agents engaged:</span>
                <div className="agent-chips">
                  {result.agents && result.agents.map((agent) => (
                    <div key={agent} className={`agent-chip ${agent}`}>
                      {renderAgentIcon(agent)}
                      <span>{agent.charAt(0).toUpperCase() + agent.slice(1)}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Final Answer Block */}
              <div className="final-answer-box glass-panel">
                <div className="box-header">
                  <h3><Check className="check-icon" size={20} /> Final Answer</h3>
                  {result.is_code && (
                    <button 
                      className={`copy-btn ${copied ? 'copied' : ''}`}
                      onClick={() => handleCopyCode(result.final_answer)}
                    >
                      {copied ? <><Check size={16}/> Copied</> : <><Copy size={16}/> Copy Code</>}
                    </button>
                  )}
                </div>
                
                <div className="answer-content">
                  {result.is_code ? (
                    <pre className="code-block">
                      <code>{result.final_answer}</code>
                    </pre>
                  ) : (
                    <div className="markdown-body">
                      <ReactMarkdown>{result.final_answer}</ReactMarkdown>
                    </div>
                  )}
                </div>
              </div>

              {/* Expandable Output Details */}
              {Object.keys(result.outputs || {}).length > 0 && (
                <div className="details-section">
                  <button 
                    className="toggle-details-btn glass-panel"
                    onClick={() => setExpandedSection(!expandedSection)}
                  >
                    <span>{expandedSection ? 'Hide' : 'View'} Internal Agent Details</span>
                    <span className="chevron">{expandedSection ? '▲' : '▼'}</span>
                  </button>

                  {expandedSection && (
                    <div className="details-grid animate-fade-in">
                      {['planner', 'research', 'coder', 'critic'].map((agentName) => {
                        const output = result.outputs[agentName];
                        if (!output) return null;
                        
                        return (
                          <div 
                            key={agentName} 
                            className={`agent-summary-card glass-panel ${agentName}`}
                            onClick={() => setPopupAgent({ name: agentName, output })}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                              {renderAgentIcon(agentName)}
                              <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>{agentName.charAt(0).toUpperCase() + agentName.slice(1)} Output</h4>
                            </div>
                            <span style={{ marginLeft: 'auto', fontSize: '0.85rem', color: 'var(--accent-color)', fontWeight: 500 }}>View Details →</span>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
          
          </> // End of new task conditional block
          )}
        </div>
      </main>

      {/* Modal Popup for Agent Output */}
      {popupAgent && (
        <div className="modal-overlay" onClick={() => setPopupAgent(null)}>
          <div 
            className={`modal-content agent-detail-card ${popupAgent.name}`} 
            onClick={(e) => e.stopPropagation()}
          >
            <div className={`detail-header modal-header ${popupAgent.name}`}>
              <div className="modal-header-title">
                {renderAgentIcon(popupAgent.name)}
                <span>{popupAgent.name.charAt(0).toUpperCase() + popupAgent.name.slice(1)} Output</span>
              </div>
              <button 
                className="close-modal-btn" 
                onClick={() => setPopupAgent(null)}
                title="Close"
              >
                ✕
              </button>
            </div>
            <div className="modal-body detail-body markdown-body">
              <ReactMarkdown>{popupAgent.output}</ReactMarkdown>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
