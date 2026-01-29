import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Globe,
  LayoutDashboard,
  Loader2,
  LogOut,
  Plus,
  ShieldCheck,
  User,
} from 'lucide-react';
import './index.css';

const App = () => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'https://ronin-archesporial-lonnie.ngrok-free.dev';

  const [status, setStatus] = useState({ type: 'info', message: 'System Ready' });
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [token, setToken] = useState('');
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('dash');
  const [saving, setSaving] = useState(false);
  const [cvUploading, setCvUploading] = useState(false);
  const [cvDeleting, setCvDeleting] = useState(false);
  const [deletingAccount, setDeletingAccount] = useState(false);
  const [roleInput, setRoleInput] = useState('');
  const cvInputRef = useRef(null);
  const loginToastShownRef = useRef(false);
  const refreshAttemptedRef = useRef(false);

  const [profileForm, setProfileForm] = useState({
    username: '',
    email: '',
    phoneNumber: '',
    city: '',
    country: '',
    jobDomain: '',
    jobRoles: [],
    jobTypes: [],
    contractTypes: [],
    notifyWhatsapp: false,
    notifyEmail: false,
    relocation: null,
    cvFileName: '',
  });

  const jobTypeOptions = [
    { id: 'REMOTE', label: 'REMOTE' },
    { id: 'HYBRID', label: 'HYBRID' },
    { id: 'ONSITE', label: 'ONSITE' },
  ];

  const contractTypeOptions = [
    { id: 'B2B', label: 'B2B' },
    { id: 'EMPLOYMENT', label: 'EMPLOYMENT' },
    { id: 'EOR', label: 'EOR' },
    { id: 'INTERNSHIP', label: 'INTERNSHIP' },
  ];

  const logMessage = useCallback((message, type = 'info') => {
    setStatus({ type, message });
  }, []);

  useEffect(() => {
    if (!status.message || status.type === 'error') {
      return;
    }
    const timeout = setTimeout(() => {
      setStatus((prev) => (prev.message ? { ...prev, message: '' } : prev));
    }, 3000);
    return () => clearTimeout(timeout);
  }, [status]);

  const fetchCsrfToken = useCallback(async () => {
    const resp = await fetch(`${baseUrl}/api/auth/csrf-token`, {
      method: 'GET',
      credentials: 'include',
    });
    const data = await resp.json();
    if (!resp.ok) {
      throw new Error(data?.message || 'Failed to fetch CSRF token');
    }
    return data;
  }, [baseUrl]);

  const refreshAuthToken = useCallback(async () => {
    const resp = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    const data = await resp.json();
    if (!resp.ok) {
      const error = new Error(data?.message || data?.error || 'Token refresh failed');
      error.status = resp.status;
      error.payload = data;
      throw error;
    }
    if (!data?.token) {
      throw new Error('Token refresh response missing token');
    }
    setToken(data.token);
    return data.token;
  }, [baseUrl]);

  const parseResponse = async (resp) => {
    const text = await resp.text();
    let parsed;
    try {
      parsed = text ? JSON.parse(text) : null;
    } catch (_) {
      parsed = text;
    }
    return parsed;
  };

  const api = useMemo(
    () => ({
      async call(path, { method = 'GET', body, headers = {}, authToken = null } = {}, retrying = false) {
        const activeToken = authToken || token;
        const requiresCsrf = activeToken && !path.startsWith('/api/auth/');
        const csrf = requiresCsrf ? await fetchCsrfToken() : null;
        const resp = await fetch(`${baseUrl}${path}`, {
          method,
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            ...(activeToken ? { Authorization: `Bearer ${activeToken}` } : {}),
            ...(csrf ? { [csrf.headerName]: csrf.token } : {}),
            ...headers,
          },
          body: body ? JSON.stringify(body) : undefined,
        });

        const parsed = await parseResponse(resp);

        if (!resp.ok) {
          const errorMessage = (parsed && parsed.message) || parsed || `Request failed with ${resp.status}`;
          const error = new Error(errorMessage);
          error.status = resp.status;
          error.payload = parsed;
          if (parsed?.error === 'UNAUTHORIZED' && !retrying && !path.startsWith('/api/auth/')) {
            const newToken = await refreshAuthToken();
            return this.call(path, { method, body, headers, authToken: newToken }, true);
          }
          throw error;
        }

        return parsed;
      },
    }),
    [baseUrl, token, fetchCsrfToken, refreshAuthToken]
  );

  useEffect(() => {
    let isMounted = true;
    if (!token) {
      if (refreshAttemptedRef.current) {
        setLoading(false);
        return;
      }
      setLoading(true);
      refreshAuthToken()
        .then((newToken) => {
          refreshAttemptedRef.current = false;
          if (isMounted) {
            setToken(newToken);
          }
        })
        .catch((err) => {
          refreshAttemptedRef.current = true;
        })
        .finally(() => {
          if (isMounted && !token) {
            setLoading(false);
          }
        });
      return () => {
        isMounted = false;
      };
    }

    let isActive = true;
    setLoading(true);
    api
      .call('/api/user/me')
      .then((data) => {
        if (!isActive) {
          return;
        }
        setUser(data);
        setProfileForm({
          username: data.username || '',
          email: data.email || '',
          phoneNumber: data.phoneNumber || '',
          city: data.city || '',
          country: data.country || '',
          jobDomain: data.jobDomain || '',
          jobRoles: data.jobRoles || [],
          jobTypes: data.jobTypes || [],
          contractTypes: data.contractTypes || [],
          notifyWhatsapp: data.notifyWhatsapp || false,
          notifyEmail: data.notifyEmail || false,
          relocation: data.relocation || null,
          cvFileName: data.cvFilename || '',
        });
        if (!loginToastShownRef.current) {
          logMessage('Secure connection established', 'success');
          loginToastShownRef.current = true;
        }
      })
      .catch((err) => {
          if (isActive) {
            logMessage(`Session expired: ${err.message}`, 'error');
            setToken('');
          }
      })
      .finally(() => {
        if (isActive) {
          setLoading(false);
        }
      });
    return () => {
      isActive = false;
    };
  }, [token, api, logMessage, refreshAuthToken]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    logMessage('Verifying credentials...', 'info');
    try {
      const res = await api.call('/api/auth/login', { method: 'POST', body: loginForm });
      setToken(res.token);
      refreshAttemptedRef.current = false;
    } catch (err) {
      setToken('');
      logMessage(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = useCallback(
    async (message = 'Logged out successfully.') => {
      const safeMessage = typeof message === 'string' ? message : 'Logged out successfully.';
      try {
        await api.call('/api/auth/logout', { method: 'POST' });
      } catch (err) {
        logMessage(err.message, 'error');
      } finally {
        setToken('');
        setUser(null);
        setLoginForm({ username: '', password: '' });
        refreshAttemptedRef.current = true;
        setLoading(false);
        logMessage(safeMessage, 'info');
        if (window.location.pathname !== '/') {
          window.history.pushState({}, '', '/');
        }
      }
    },
    [api, logMessage]
  );

  useEffect(() => {
    if (!token) {
      return undefined;
    }
    let timeoutId;
    const scheduleLogout = () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
      timeoutId = setTimeout(() => {
        handleLogout('Signed out due to inactivity.');
      }, 30 * 60 * 1000);
    };

    const handleActivity = () => {
      scheduleLogout();
    };

    scheduleLogout();
    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
    events.forEach((event) => window.addEventListener(event, handleActivity, { passive: true }));

    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
      events.forEach((event) => window.removeEventListener(event, handleActivity));
    };
  }, [token, handleLogout, logMessage]);

  const tabToPath = {
    profile: '/profile',
    dash: '/dashboard',
  };

  const pathToTab = (path) => {
    if (path === '/profile') return 'profile';
    if (path === '/dashboard') return 'dash';
    return null;
  };

  const navigateToTab = useCallback((tab) => {
    setActiveTab(tab);
    const nextPath = tabToPath[tab];
    if (nextPath && window.location.pathname !== nextPath) {
      window.history.pushState({ tab }, '', nextPath);
    }
  }, []);

  useEffect(() => {
    const applyPath = () => {
      const tab = pathToTab(window.location.pathname);
      if (tab) {
        setActiveTab(tab);
      }
    };
    applyPath();
    window.addEventListener('popstate', applyPath);
    return () => window.removeEventListener('popstate', applyPath);
  }, []);

  const SidebarItem = ({ icon: Icon, label, id, active }) => (
    <button
      onClick={() => navigateToTab(id)}
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group ${
        active ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-100' : 'text-slate-500 hover:bg-slate-50 hover:text-indigo-600'
      }`}
    >
      <Icon size={18} className={active ? 'text-white' : 'text-slate-400 group-hover:text-indigo-500'} />
      <span className="font-semibold text-sm">{label}</span>
    </button>
  );

  const ToggleSwitch = ({ label, checked, onChange }) => (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="inline-flex items-center gap-3 rounded-full px-3 py-2 bg-slate-100 hover:bg-slate-200 transition-colors"
    >
      <span className={`text-xs font-semibold uppercase tracking-wide ${checked ? 'text-slate-700' : 'text-slate-400'}`}>
        {label}
      </span>
      <span
        className={`relative w-12 h-6 rounded-full transition-colors ${checked ? 'bg-indigo-700' : 'bg-slate-300'}`}
      >
        <span
          className={`absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
            checked ? 'translate-x-6' : 'translate-x-0'
          }`}
        />
      </span>
    </button>
  );

  const StatusToast = () => {
    if (!status.message) {
      return null;
    }
    return (
    <div
      className={`fixed bottom-6 right-6 flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border transition-all duration-300 transform z-50 ${
        status.type === 'error'
          ? 'bg-red-50 border-red-100 text-red-700'
          : status.type === 'success'
            ? 'bg-emerald-50 border-emerald-100 text-emerald-700'
            : 'bg-white border-slate-100 text-slate-600'
      }`}
    >
      {status.type === 'error' && <AlertCircle size={18} />}
      {status.type === 'success' && <CheckCircle2 size={18} />}
      <span className="text-sm font-medium">{status.message}</span>
    </div>
    );
  };

  const handleProfileChange = (field, value) => {
    setProfileForm((prev) => ({ ...prev, [field]: value }));
  };

  const toggleJobType = (type) => {
    setProfileForm((prev) => {
      const has = prev.jobTypes.includes(type);
      return { ...prev, jobTypes: has ? prev.jobTypes.filter((t) => t !== type) : [...prev.jobTypes, type] };
    });
  };

  const toggleContractType = (type) => {
    setProfileForm((prev) => {
      const has = prev.contractTypes.includes(type);
      return { ...prev, contractTypes: has ? prev.contractTypes.filter((t) => t !== type) : [...prev.contractTypes, type] };
    });
  };

  const handleAddRole = () => {
    const role = roleInput.trim();
    if (!role) return;
    setProfileForm((prev) => ({
      ...prev,
      jobRoles: prev.jobRoles.includes(role) ? prev.jobRoles : [...prev.jobRoles, role],
    }));
    setRoleInput('');
  };

  const handleRemoveRole = (index) => {
    setProfileForm((prev) => ({
      ...prev,
      jobRoles: prev.jobRoles.filter((_, i) => i !== index),
    }));
  };

  const handleUpdateRole = (index, value) => {
    setProfileForm((prev) => {
      const nextRoles = [...prev.jobRoles];
      nextRoles[index] = value;
      return { ...prev, jobRoles: nextRoles };
    });
  };

  const handleSaveProfile = async () => {
    if (!profileForm.phoneNumber) {
      logMessage('Phone number is required', 'error');
      return;
    }
    setSaving(true);
    try {
      await api.call('/api/user/update', {
        method: 'PUT',
        body: {
          username: profileForm.username || user?.username,
          phoneNumber: profileForm.phoneNumber,
          notifyWhatsapp: profileForm.notifyWhatsapp,
          notifyEmail: profileForm.notifyEmail,
          city: profileForm.city || null,
          country: profileForm.country || null,
          jobDomain: profileForm.jobDomain || null,
          jobRoles: profileForm.jobRoles,
          jobTypes: profileForm.jobTypes,
          relocation: profileForm.relocation,
          contractTypes: profileForm.contractTypes,
          aiPrompts: null,
        },
      });
      logMessage('Profile updated', 'success');
      setUser((prev) => ({
        ...(prev || {}),
        username: profileForm.username || prev?.username,
        email: profileForm.email || prev?.email,
        phoneNumber: profileForm.phoneNumber,
        notifyWhatsapp: profileForm.notifyWhatsapp,
        notifyEmail: profileForm.notifyEmail,
        city: profileForm.city || null,
        country: profileForm.country || null,
        jobDomain: profileForm.jobDomain || null,
        jobRoles: profileForm.jobRoles,
        jobTypes: profileForm.jobTypes,
        relocation: profileForm.relocation,
        contractTypes: profileForm.contractTypes,
      }));
    } catch (err) {
      logMessage(err.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleUploadCv = async (file) => {
    if (!file) return;
    setCvUploading(true);
    try {
      const uploadOnce = async (authToken) => {
        const csrf = await fetchCsrfToken();
        const formData = new FormData();
        formData.append('file', file);
        return fetch(`${baseUrl}/api/cv/upload`, {
          method: 'POST',
          credentials: 'include',
          headers: {
            ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
            [csrf.headerName]: csrf.token,
          },
          body: formData,
        });
      };

      const resp = await uploadOnce(token);
      const parsed = await parseResponse(resp);
      if (!resp.ok) {
        if (parsed?.error === 'UNAUTHORIZED') {
          const newToken = await refreshAuthToken();
          const retryResp = await uploadOnce(newToken);
          const retryParsed = await parseResponse(retryResp);
          if (!retryResp.ok) {
            throw new Error((retryParsed && retryParsed.message) || retryParsed || 'CV upload failed');
          }
        } else {
          throw new Error((parsed && parsed.message) || parsed || 'CV upload failed');
        }
      }
      logMessage('CV uploaded', 'success');
      setProfileForm((prev) => ({ ...prev, cvFileName: file.name }));
    } catch (err) {
      logMessage(err.message, 'error');
    } finally {
      setCvUploading(false);
      if (cvInputRef.current) {
        cvInputRef.current.value = '';
      }
    }
  };

  const handleDeleteCv = async () => {
    setCvDeleting(true);
    try {
      await api.call('/api/cv', { method: 'DELETE' });
      logMessage('CV deleted', 'success');
      setProfileForm((prev) => ({ ...prev, cvFileName: '' }));
    } catch (err) {
      logMessage(err.message, 'error');
    } finally {
      setCvDeleting(false);
    }
  };

  const handleDownloadCv = () => {
    logMessage('CV download is not available yet', 'info');
  };

  const handleDeleteAccount = async () => {
    const confirmed = window.confirm('Delete your account? This cannot be undone.');
    if (!confirmed) return;
    setDeletingAccount(true);
    try {
      await api.call('/api/user/delete', { method: 'DELETE' });
      logMessage('Account deleted', 'success');
      handleLogout();
    } catch (err) {
      logMessage(err.message, 'error');
    } finally {
      setDeletingAccount(false);
    }
  };

  if (!user && !loading && !token) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 font-sans">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center mb-4">
              <img
                src="/images/JobsHunterLogo.png"
                alt="JobsHunter logo"
                className="max-w-[512px] w-48 h-auto drop-shadow-xl"
              />
            </div>
            <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">JobsHunter</h1>
            <p className="text-slate-500 mt-2 font-medium">Your gateway to the next career leap</p>
            <h2 className="text-1xl font-extrabold text-slate-700 tracking-tight">Put AI to hunt jobs for you</h2>
          </div>

          <div className="bg-white rounded-3xl shadow-2xl shadow-slate-200/60 p-8 border border-slate-100">
            <form onSubmit={handleLogin} className="space-y-6">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-2 ml-1">Username</label>
                <input
                  type="text"
                  required
                  placeholder="Enter your username"
                  className="w-full px-4 py-3.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 outline-none transition-all"
                  value={loginForm.username}
                  onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-2 ml-1">Password</label>
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  className="w-full px-4 py-3.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 outline-none transition-all"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-4 rounded-xl shadow-lg shadow-indigo-100 transition-all flex items-center justify-center gap-2 active:scale-[0.98] disabled:opacity-70"
              >
                {loading ? <Loader2 className="animate-spin" size={20} /> : 'Login'}
              </button>
            </form>
          </div>
          <StatusToast />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans">
      <aside className="w-64 bg-white border-r border-slate-200 hidden lg:flex flex-col p-6 shrink-0">
        <div className="flex items-center gap-3 mb-10 px-2">
          <div className="w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-100">
            <Globe className="text-white" size={20} />
          </div>
          <span className="font-black text-xl text-slate-900 tracking-tight">JobsHunter</span>
        </div>

        <nav className="flex-1 space-y-1.5">
          <SidebarItem icon={User} label="Profile" id="profile" active={activeTab === 'profile'} />
          <SidebarItem icon={LayoutDashboard} label="Dashboard" id="dash" active={activeTab === 'dash'} />
        </nav>

        <div className="pt-6 border-t border-slate-100">
          <div className="bg-slate-50 rounded-2xl p-4 mb-4">
            <p className="text-xs font-bold text-slate-400 mb-1 uppercase tracking-tight text-center">Connected as</p>
            <p className="text-sm font-bold text-slate-900 text-center truncate">{user?.username}</p>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-xl text-slate-500 hover:bg-red-50 hover:text-red-600 transition-all font-bold text-sm"
          >
            <LogOut size={18} />
            Sign Out
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col h-screen overflow-hidden">
        <div className="lg:hidden bg-white border-b border-slate-200">
          <div className="flex items-center justify-between px-4 py-3">
            <button
              onClick={() => navigateToTab('profile')}
              className={`flex-1 text-sm font-semibold py-2 px-3 rounded-full transition ${
                activeTab === 'profile' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'
              }`}
            >
              Profile
            </button>
            <button
              onClick={() => navigateToTab('dash')}
              className={`flex-1 ml-2 text-sm font-semibold py-2 px-3 rounded-full transition ${
                activeTab === 'dash' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'
              }`}
            >
              Dashboard
            </button>
            <button
              onClick={() => handleLogout()}
              className="flex-1 ml-2 text-sm font-semibold py-2 px-3 rounded-full bg-slate-100 text-slate-600"
            >
              Sign out
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto bg-slate-50/50 p-8">
          {loading ? (
            <div className="h-full flex flex-col items-center justify-center text-slate-400 gap-4">
              <div className="relative">
                <div className="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin" />
              </div>
              <p className="font-bold text-sm tracking-wide">Fetching secure data...</p>
            </div>
          ) : (
            <div className="max-w-5xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
              {activeTab === 'dash' && (
                <>
                  <section className="relative overflow-hidden bg-gradient-to-br from-indigo-900 to-indigo-800 rounded-[2.5rem] p-10 text-white shadow-2xl shadow-indigo-200">
                    <div className="relative z-10 grid md:grid-cols-2 gap-8 items-center">
                      <div>
                        <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-indigo-100 text-[10px] font-black uppercase tracking-widest mb-4 border border-white/5 backdrop-blur-sm">
                          <ShieldCheck size={12} /> Live Account
                        </span>
                        <h1 className="text-4xl font-extrabold mb-3 tracking-tight">Success is near, {user?.username}.</h1>
                        <p className="text-indigo-100/80 text-lg font-medium leading-relaxed">You have 3 active interviews scheduled for this week. Good luck!</p>
                      </div>
                      <div className="hidden md:flex justify-end">
                        <div className="grid grid-cols-2 gap-4">
                          {[{ label: 'Applications', val: '24' }, { label: 'Interviews', val: '3' }, { label: 'Offers', val: '1' }, { label: 'Views', val: '142' }].map((stat) => (
                            <div key={stat.label} className="bg-white/5 backdrop-blur-md border border-white/10 p-5 rounded-3xl min-w-[120px]">
                              <p className="text-indigo-200 text-[10px] font-bold uppercase tracking-widest mb-1">{stat.label}</p>
                              <p className="text-2xl font-black">{stat.val}</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="absolute -right-16 -top-16 w-80 h-80 bg-white/5 rounded-full blur-3xl" />
                    <div className="absolute -left-16 -bottom-16 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl" />
                  </section>
                </>
              )}

              {activeTab === 'profile' && (
                <div className="space-y-8 max-w-4xl mx-auto">
                  <div className="bg-white rounded-[2rem] border border-slate-200 shadow-sm p-8 space-y-8">
                    <div className="space-y-3">
                      <h2 className="text-xl font-bold text-slate-900">Profile</h2>
                      <p className="text-sm text-slate-500">Manage your identity, preferences, and notifications.</p>
                    </div>

                    <div className="space-y-6">
                      <div className="grid lg:grid-cols-[1fr_auto_1fr] gap-6 items-start">
                        <div className="space-y-3">
                          <h3 className="text-lg font-semibold text-slate-900">Identity</h3>
                          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2 text-sm text-slate-700">
                            <p>
                              <span className="font-semibold text-slate-800">Username:</span> {user?.username || '-'}
                            </p>
                            <p>
                              <span className="font-semibold text-slate-800">Email:</span> {user?.email || '-'}
                            </p>
                            <p>
                              <span className="font-semibold text-slate-800">Phone:</span> {user?.phoneNumber || '-'}
                            </p>
                          </div>
                        </div>

                        <div className="hidden lg:block w-px bg-slate-200 h-full" />

                        <div className="space-y-3">
                          <h3 className="text-lg font-semibold text-slate-900">CV/Resume</h3>
                          <div className="space-y-3">
                            <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-sm text-slate-600">
                              {profileForm.cvFileName || 'No CV uploaded'}
                            </div>
                            <div className="flex flex-wrap gap-2">
                              <input
                                ref={cvInputRef}
                                type="file"
                                accept=".pdf,.doc,.docx,.txt"
                                className="hidden"
                                onChange={(e) => handleUploadCv(e.target.files?.[0])}
                              />
                              <button
                                onClick={() => cvInputRef.current?.click()}
                                disabled={cvUploading}
                                className="px-4 py-2 rounded-lg bg-indigo-700 text-white text-sm font-semibold disabled:opacity-70"
                              >
                                {cvUploading ? 'Uploading...' : 'Upload'}
                              </button>
                              <button
                                onClick={handleDownloadCv}
                                className="px-4 py-2 rounded-lg border border-slate-200 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                              >
                                Download
                              </button>
                              <button
                                onClick={handleDeleteCv}
                                disabled={cvDeleting}
                                className="px-4 py-2 rounded-lg bg-red-100 text-red-700 text-sm font-semibold disabled:opacity-70"
                              >
                                {cvDeleting ? 'Deleting...' : 'Delete'}
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className="border-t border-slate-200" />

                      <div className="space-y-3">
                        <h3 className="text-lg font-semibold text-slate-900">Location</h3>
                        <div className="grid sm:grid-cols-2 gap-4">
                          <div>
                            <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">City</p>
                            <input
                              value={profileForm.city}
                              onChange={(e) => handleProfileChange('city', e.target.value)}
                              className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                              placeholder="Select city"
                            />
                          </div>
                          <div>
                            <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">Country</p>
                            <input
                              value={profileForm.country}
                              onChange={(e) => handleProfileChange('country', e.target.value)}
                              className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                              placeholder="Select country"
                            />
                          </div>
                        </div>
                      </div>

                      <div className="border-t border-slate-200" />

                      <div className="space-y-3">
                        <h3 className="text-lg font-semibold text-slate-900">Job Preferences</h3>
                        <div className="space-y-4">
                          <div>
                            <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">Domain</p>
                            <input
                              value={profileForm.jobDomain}
                              onChange={(e) => handleProfileChange('jobDomain', e.target.value)}
                              className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                              placeholder="Software Engineering"
                            />
                          </div>

                          <div>
                            <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">Roles</p>
                          </div>

                          <div className="border border-slate-200 rounded-2xl bg-slate-50">
                            <div className="p-4 space-y-3">
                              {profileForm.jobRoles.length === 0 && <p className="text-sm text-slate-500">No roles added yet.</p>}
                              {profileForm.jobRoles.map((role, index) => (
                                <div key={index} className="flex items-center justify-between text-sm text-slate-800 bg-white border border-slate-200 rounded-xl px-3 py-2 gap-3">
                                  <input
                                    value={role}
                                    onChange={(e) => handleUpdateRole(index, e.target.value)}
                                    className="flex-1 bg-transparent text-sm outline-none"
                                  />
                                  <button onClick={() => handleRemoveRole(index)} className="text-indigo-600 font-semibold text-xs hover:underline">
                                    Delete
                                  </button>
                                </div>
                              ))}
                              <div className="flex flex-col sm:flex-row gap-2">
                                <input
                                  value={roleInput}
                                  onChange={(e) => setRoleInput(e.target.value)}
                                  className="flex-1 px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                  placeholder="Add role"
                                />
                                <button
                                  onClick={handleAddRole}
                                  className="px-4 py-2 rounded-lg bg-indigo-700 text-white text-sm font-semibold"
                                >
                                  + Add role
                                </button>
                              </div>
                            </div>
                          </div>

                          <div className="space-y-4">
                            <div>
                              <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-2">Job types</p>
                              <div className="flex flex-wrap gap-3">
                                {jobTypeOptions.map((opt) => {
                                  const active = profileForm.jobTypes.includes(opt.id);
                                  return (
                                    <ToggleSwitch
                                      key={opt.id}
                                      label={opt.label}
                                      checked={active}
                                      onChange={() => toggleJobType(opt.id)}
                                    />
                                  );
                                })}
                              </div>
                            </div>

                            <div>
                              <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-2">Contract types</p>
                              <div className="flex flex-wrap gap-3">
                                {contractTypeOptions.map((opt) => {
                                  const active = profileForm.contractTypes.includes(opt.id);
                                  return (
                                    <ToggleSwitch
                                      key={opt.id}
                                      label={opt.label}
                                      checked={active}
                                      onChange={() => toggleContractType(opt.id)}
                                    />
                                  );
                                })}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className="border-t border-slate-200" />

                      <div className="space-y-3">
                        <h3 className="text-lg font-semibold text-slate-900">Notifications</h3>
                        <div className="flex flex-wrap gap-3">
                          <ToggleSwitch
                            label="WhatsApp"
                            checked={profileForm.notifyWhatsapp}
                            onChange={(next) => handleProfileChange('notifyWhatsapp', next)}
                          />
                          <ToggleSwitch
                            label="Email"
                            checked={profileForm.notifyEmail}
                            onChange={(next) => handleProfileChange('notifyEmail', next)}
                          />
                        </div>
                      </div>

                      <div className="border-t border-slate-200" />

                      <div className="flex flex-wrap items-center justify-between gap-4 pt-2">
                        <button
                          onClick={handleSaveProfile}
                          disabled={saving}
                          className="w-64 px-6 py-3 rounded-xl bg-indigo-700 text-white font-semibold text-sm shadow-lg shadow-indigo-200 disabled:opacity-70 flex items-center justify-center gap-2"
                        >
                          {saving ? (
                            'Saving...'
                          ) : (
                            <>
                              <CheckCircle2 size={18} />
                              Save
                            </>
                          )}
                        </button>
                        <button
                          onClick={handleDeleteAccount}
                          disabled={deletingAccount}
                          className="text-red-600 text-sm font-semibold underline disabled:opacity-70"
                        >
                          {deletingAccount ? 'Deleting...' : 'Delete account'}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </main>

      <StatusToast />
    </div>
  );
};

export default App;
