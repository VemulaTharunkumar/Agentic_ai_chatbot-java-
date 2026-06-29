import React, { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import { LogOut, Copy, Send, Bot, Check, LayoutDashboard, Brain, Globe, Code, FileText, Trash2, Star, Edit2, X } from 'lucide-react';
import ThinkingLoader from './ThinkingLoader';
import './Dashboard.css';
const API_URL = "http://127.0.0.1:8000";
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
  const [editingChatId, setEditingChatId] = useState(null);
  const [editingTitle, setEditingTitle] = useState('');
  const [animatingChatId, setAnimatingChatId] = useState(null);
  const [deletingChatId, setDeletingChatId] = useState(null);
  const [erasingChatId, setErasingChatId] = useState(null);

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
      const response = await fetch('http://127.0.0.1:8000/api/task', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task, username: user.username })
      });

      const data = await response.json();

      if (response.ok) {
        setResult(data);
        
        // Append to chat history, but maintain max 10
        const newHistoryItem = {
          _id: data.chat_id || Date.now().toString(), // Use actual DB ID
          prompt: task,
          title: task,
          isPinned: false,
          response: data.final_answer,
          agent: data.agents.join(", "),
          timestamp: new Date().toISOString()
        };
        
        setChatHistory(prev => {
          const updated = [...prev, newHistoryItem];
          if (updated.length > 10) {
            // Find the oldest unpinned chat to remove
            const unpinnedChats = updated.filter(c => !c.isPinned);
            if (unpinnedChats.length > 0) {
              const oldestUnpinnedId = unpinnedChats.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))[0]._id;
              return updated.filter(c => c._id !== oldestUnpinnedId);
            } else {
              // If all are pinned (rare), remove oldest overall
              const oldestOverallId = updated.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))[0]._id;
              return updated.filter(c => c._id !== oldestOverallId);
            }
          }
          return updated;
        });
        
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
    e.stopPropagation();

    if (!window.confirm("Are you sure you want to delete this chat history?")) return;

    // Start delete animation
    setDeletingChatId(chatId);

    try {
      const res = await fetch(`http://127.0.0.1:8000/api/history/${chatId}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        setTimeout(() => {
          setChatHistory(prev => prev.filter(c => c._id !== chatId));
          if (selectedHistoryItem?._id === chatId) {
            setSelectedHistoryItem(null);
          }
          setDeletingChatId(null);
        }, 300); // 300ms for slide-out animation
      } else {
        console.error("Failed to delete chat record");
        setDeletingChatId(null);
      }
    } catch (err) {
      console.error("Could not reach backend to delete chat", err);
      setDeletingChatId(null);
    }
  };

  const handleTogglePin = async (e, chatId, currentStatus) => {
    e.stopPropagation();
    try {
      const res = await fetch(`http://127.0.0.1:8000/api/history/${chatId}/pin`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isPinned: !currentStatus })
      });
      
      if (res.ok) {
        setChatHistory(prev => {
          const updated = prev.map(c => c._id === chatId ? { ...c, isPinned: !currentStatus } : c);
          // Re-sort: pinned first, then by timestamp desc
          return updated.sort((a, b) => {
            if (a.isPinned === b.isPinned) {
              return new Date(b.timestamp) - new Date(a.timestamp);
            }
            return a.isPinned ? -1 : 1;
          });
        });
      } else if (res.status === 400) {
        const errorData = await res.json();
        window.alert(errorData.detail || "Maximum of 10 pinned chats allowed.");
      }
    } catch (err) {
      console.error("Could not reach backend to pin chat", err);
    }
  };

  const handleRenameSubmit = async (e, chatId) => {
    e?.preventDefault();
    e?.stopPropagation();
    if (!editingTitle.trim()) {
      setEditingChatId(null);
      return;
    }

    // Capture the target ID and immediately close the input form
    const targetId = chatId;
    setEditingChatId(null);
    
    // Start "erasing" animation on the old title
    setErasingChatId(targetId);

    try {
      const res = await fetch(`http://127.0.0.1:8000/api/history/${targetId}/rename`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: editingTitle })
      });
      
      if (res.ok) {
        setTimeout(() => {
          // Erasing finished (300ms). Now update title and start "typing" animation.
          setErasingChatId(null);
          setChatHistory(prev => prev.map(c => c._id === targetId ? { ...c, title: editingTitle } : c));
          setAnimatingChatId(targetId);
          
          setTimeout(() => setAnimatingChatId(null), 500); // 500ms for typing animation
        }, 300);
      } else {
        setErasingChatId(null);
      }
    } catch (err) {
      console.error("Could not reach backend to rename chat", err);
      setErasingChatId(null);
    }
  };

  const startRename = (e, chat) => {
    e.stopPropagation();
    setEditingTitle(chat.title || chat.prompt);
    setEditingChatId(chat._id);
  };

  const handleKeyDown = (e, chatId) => {
    if (e.key === 'Escape') {
      setEditingChatId(null);
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
                  className={`history-list-item ${selectedHistoryItem?._id === chat._id ? 'active' : ''} ${chat.isPinned ? 'pinned' : ''} ${deletingChatId === chat._id ? 'deleting-animation' : ''}`}
                  onClick={() => setSelectedHistoryItem(chat)}
                >
                  <Bot size={14} className="history-icon" />
                  
                  {editingChatId === chat._id ? (
                    <form onSubmit={(e) => handleRenameSubmit(e, chat._id)} className="rename-form" onClick={(e) => e.stopPropagation()}>
                      <input 
                        type="text" 
                        value={editingTitle} 
                        onChange={(e) => setEditingTitle(e.target.value)} 
                        autoFocus
                        onKeyDown={(e) => handleKeyDown(e, chat._id)}
                      />
                    </form>
                  ) : (
                    <span className={`history-truncate ${animatingChatId === chat._id ? 'typing-animation' : ''} ${erasingChatId === chat._id ? 'erasing-animation' : ''}`}>
                      {chat.title || chat.prompt}
                    </span>
                  )}
                  
                  <div className="history-actions">
                    <button 
                      className={`action-btn pin-btn ${chat.isPinned ? 'is-pinned' : ''}`}
                      onClick={(e) => handleTogglePin(e, chat._id, chat.isPinned)}
                      title={chat.isPinned ? "Unpin Chat" : "Pin Chat"}
                    >
                      <Star size={14} fill={chat.isPinned ? "currentColor" : "none"} />
                    </button>
                    <button 
                      className="action-btn edit-btn"
                      onClick={(e) => startRename(e, chat)}
                      title="Rename Chat"
                    >
                      <Edit2 size={14} />
                    </button>
                    <button 
                      className="action-btn delete-chat-btn"
                      onClick={(e) => handleDeleteChat(e, chat._id)}
                      title="Delete Chat"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
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
