import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Globe,
  LayoutDashboard,
  Loader2,
  LogOut,
  Plus,
  Trash2,
  User,
} from 'lucide-react';
import './index.css';
import { buildApiUrl, resolveApiBase } from './config/apiConfig';

const App = () => {
  const apiBase = resolveApiBase();

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
  const [confirmDeleteCv, setConfirmDeleteCv] = useState(false);
  const [confirmDeleteAccount, setConfirmDeleteAccount] = useState(false);
  const [roleInput, setRoleInput] = useState('');
  const [orders, setOrders] = useState([]);
  const [jobsFound, setJobsFound] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [jobsLoading, setJobsLoading] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [ordersSort, setOrdersSort] = useState({ key: 'modifiedAt', direction: 'desc' });
  const [ordersPage, setOrdersPage] = useState(1);
  const [jobsSort, setJobsSort] = useState({ key: 'createdAt', direction: 'desc' });
  const [countries, setCountries] = useState([]);
  const [countriesLoading, setCountriesLoading] = useState(false);
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
    aiPrompts: [],
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

  const relocationOptions = [
    { value: 'NO', label: 'No' },
    { value: 'YES_BUT_WITHIN_COUNTRY', label: 'Yes, but only within country' },
    { value: 'YES', label: 'Yes' }
  ];

  const JobsHunterLogoSrc = '/images/JobsHunterLogo.jpg';

  const orderColumns = [
    { key: 'modifiedAt', label: 'Date', align: 'left' },
    { key: 'provider', label: 'Engine', align: 'left' },
    { key: 'model', label: 'Model', align: 'left' },
    { key: 'searchCompanies', label: 'By company', align: 'center' },
    { key: 'searchByPrompts', label: 'By prompts', align: 'center' },
    { key: 'status', label: 'Status', align: 'left' },
    { key: 'jobsFound', label: 'Jobs found', align: 'left' },
    { key: 'errorMessage', label: 'Error', align: 'left' },
  ];

  const sortedOrders = useMemo(() => {
    const sorted = [...orders];
    const { key, direction } = ordersSort;
    const multiplier = direction === 'asc' ? 1 : -1;
    sorted.sort((a, b) => {
      const getValue = (order) => {
        switch (key) {
          case 'modifiedAt':
            return order.modifiedAt ? new Date(order.modifiedAt).getTime() : 0;
          case 'provider':
            return order.provider || '';
          case 'model':
            return order.model || '';
          case 'searchCompanies':
            return order.searchCompanies ? 1 : 0;
          case 'searchByPrompts':
            return order.searchByPrompts ? 1 : 0;
          case 'status':
            return order.status || '';
          case 'jobsFound':
            return order.jobsFound || 0;
          case 'errorMessage':
            return order.errorMessage || '';
          default:
            return '';
        }
      };
      const aVal = getValue(a);
      const bVal = getValue(b);
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return (aVal - bVal) * multiplier;
      }
      return String(aVal).localeCompare(String(bVal)) * multiplier;
    });
    return sorted;
  }, [orders, ordersSort]);

  const selectedOrder = useMemo(
    () => orders.find((order) => order.id === selectedOrderId) || null,
    [orders, selectedOrderId]
  );

  const ordersPerPage = 5;
  const totalOrderPages = Math.max(1, Math.ceil(sortedOrders.length / ordersPerPage));
  const pagedOrders = sortedOrders.slice((ordersPage - 1) * ordersPerPage, ordersPage * ordersPerPage);

  const handleOrderSort = (key) => {
    setOrdersSort((prev) => {
      if (prev.key === key) {
        return { key, direction: prev.direction === 'asc' ? 'desc' : 'asc' };
      }
      return { key, direction: 'asc' };
    });
  };

  const sortArrow = (key) => {
    if (ordersSort.key !== key) return '';
    return ordersSort.direction === 'asc' ? '↑' : '↓';
  };

  const sortedJobs = useMemo(() => {
    const sorted = [...jobsFound];
    const { key, direction } = jobsSort;
    const multiplier = direction === 'asc' ? 1 : -1;
    sorted.sort((a, b) => {
      const getValue = (job) => {
        switch (key) {
          case 'url':
            return job.url || '';
          case 'createdAt':
            return job.createdAt ? new Date(job.createdAt).getTime() : 0;
          case 'engine':
            return selectedOrder?.provider || '';
          case 'model':
            return selectedOrder?.model || '';
          default:
            return '';
        }
      };
      const aVal = getValue(a);
      const bVal = getValue(b);
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return (aVal - bVal) * multiplier;
      }
      return String(aVal).localeCompare(String(bVal)) * multiplier;
    });
    return sorted;
  }, [jobsFound, jobsSort, selectedOrder]);

  const handleJobsSort = (key) => {
    setJobsSort((prev) => {
      if (prev.key === key) {
        return { key, direction: prev.direction === 'asc' ? 'desc' : 'asc' };
      }
      return { key, direction: 'asc' };
    });
  };

  const jobsSortArrow = (key) => {
    if (jobsSort.key !== key) return '';
    return jobsSort.direction === 'asc' ? '↑' : '↓';
  };

  const bannerMessage = useMemo(() => {
    const hasNew = orders.some((order) => order.status === 'NEW');
    const hasProcessing = orders.some((order) => order.status === 'PROCESSING');
    if (!hasNew && !hasProcessing) {
      return 'No searches are running right now. Start a new order whenever you’re ready.';
    }
    if (hasNew) {
      return 'Your order is queued and will start automatically when resources become available.';
    }
    return 'Your searches are running in the background. Check back anytime for new results.';
  }, [orders]);

  const logMessage = useCallback((message, type = 'info') => {
    setStatus({ type, message });
  }, []);

  useEffect(() => {
    if (!status.message) {
      return;
    }
    const timeout = setTimeout(() => {
      setStatus((prev) => (prev.message ? { ...prev, message: '' } : prev));
    }, 3000);
    return () => clearTimeout(timeout);
  }, [status]);

  // Handle OAuth2 return from Google SSO
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oauthSuccess = params.get('oauth_success');
    const accessToken = params.get('access_token');
    const oauthError = params.get('error');

    // Clean up URL parameters after processing
    const cleanUrl = () => {
      const url = new URL(window.location.href);
      url.searchParams.delete('oauth_success');
      url.searchParams.delete('access_token');
      url.searchParams.delete('error');
      window.history.replaceState({}, '', url.pathname + url.search);
    };

    if (oauthError) {
      logMessage(`Login failed: ${oauthError}`, 'error');
      cleanUrl();
      return;
    }

    if (oauthSuccess === 'true' && accessToken) {
      setToken(accessToken);
      logMessage('Successfully signed in with Google', 'success');
      loginToastShownRef.current = true;
      cleanUrl();
    }
  }, [logMessage]);

  const fetchCsrfToken = useCallback(async () => {
    const resp = await fetch(buildApiUrl(apiBase, '/auth/csrf-token'), {
      method: 'GET',
      credentials: 'include',
    });
    const data = await resp.json();
    if (!resp.ok) {
      throw new Error(data?.message || 'Failed to fetch CSRF token');
    }
    return data;
  }, [apiBase]);

  const refreshAuthToken = useCallback(async () => {
    const resp = await fetch(buildApiUrl(apiBase, '/auth/refresh'), {
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
  }, [apiBase]);

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
        const requiresCsrf = activeToken && !path.startsWith('/auth/');
        const csrf = requiresCsrf ? await fetchCsrfToken() : null;
        const resp = await fetch(buildApiUrl(apiBase, path), {
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
          if (parsed?.error === 'UNAUTHORIZED' && !retrying && !path.startsWith('/auth/')) {
            const newToken = await refreshAuthToken();
            return this.call(path, { method, body, headers, authToken: newToken }, true);
          }
          throw error;
        }

        return parsed;
      },
    }),
    [apiBase, token, fetchCsrfToken, refreshAuthToken]
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
      .call('/user/me')
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
          aiPrompts: (data.prompts || []).map((prompt) => ({
            prompt,
          })),
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

  useEffect(() => {
    if (!token || activeTab !== 'dash') {
      return;
    }
    let active = true;
    setOrdersLoading(true);
    api
      .call('/engine/orders')
      .then((data) => {
        const list = Array.isArray(data) ? data : [];
        list.sort((a, b) => new Date(b.modifiedAt).getTime() - new Date(a.modifiedAt).getTime());
        if (active) {
          setOrders(list);
        }
      })
      .catch((err) => {
        if (active) {
          logMessage(err.message, 'error');
          setOrders([]);
        }
      })
      .finally(() => {
        if (active) {
          setOrdersLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [token, activeTab, api, logMessage]);

  const loadJobs = useCallback(
    async (orderId = null) => {
      if (!token || !orderId) {
        setJobsFound([]);
        return;
      }
      setJobsLoading(true);
      try {
        const params = orderId ? `?orderId=${encodeURIComponent(orderId)}` : '';
        const data = await api.call(`/user/jobs${params}`);
        setJobsFound(Array.isArray(data) ? data : []);
      } catch (err) {
        logMessage(err.message, 'error');
        setJobsFound([]);
      } finally {
        setJobsLoading(false);
      }
    },
    [api, logMessage, token]
  );

  useEffect(() => {
    if (activeTab !== 'dash') {
      return;
    }
    loadJobs(selectedOrderId);
  }, [activeTab, selectedOrderId, loadJobs]);

  useEffect(() => {
    if (!token) {
      setCountries([]);
      return;
    }
    setCountriesLoading(true);
    api
      .call('/misc/countries')
      .then((data) => {
        const items = Array.isArray(data) ? data : [];
        setCountries(items);
      })
      .catch((err) => {
        logMessage(err.message, 'error');
        setCountries([]);
      })
      .finally(() => setCountriesLoading(false));
  }, [token, api, logMessage]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    logMessage('Verifying credentials...', 'info');
    try {
      const res = await api.call('/auth/login', { method: 'POST', body: loginForm });
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
        await api.call('/auth/logout', { method: 'POST' });
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

  // Keyboard shortcuts for navigation (P for Profile, D for Dashboard)
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Don't trigger if user is typing in an input/textarea
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
        return;
      }
      // Don't trigger if user is not logged in
      if (!user) {
        return;
      }
      
      if (e.key === 'p' || e.key === 'P') {
        navigateToTab('profile');
      } else if (e.key === 'd' || e.key === 'D') {
        navigateToTab('dash');
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [user, navigateToTab]);

  const SidebarItem = ({ icon: Icon, label, id, active }) => (
    <button
      onClick={() => navigateToTab(id)}
      aria-current={active ? 'page' : undefined}
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group ${
        active 
          ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-100' 
          : 'text-slate-500 hover:bg-indigo-50 hover:text-indigo-600 hover:scale-[1.02] hover:shadow-sm'
      }`}
    >
      <Icon size={18} className={`transition-transform duration-200 ${active ? 'text-white' : 'text-slate-400 group-hover:text-indigo-500 group-hover:scale-110'}`} />
      <span className="font-semibold text-sm">{label}</span>
      <span className="ml-auto text-xs text-slate-400 opacity-0 group-hover:opacity-100 transition-opacity">
        {id === 'profile' ? 'P' : 'D'}
      </span>
    </button>
  );

  const ConfirmDialog = ({ open, title, message, confirmLabel, onConfirm, onCancel, destructive }) => {
    if (!open) return null;
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
        <div className="bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full mx-4 space-y-4">
          <h3 className="text-lg font-bold text-slate-900">{title}</h3>
          <p className="text-sm text-slate-600">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="px-4 py-2 rounded-lg border border-slate-200 text-sm font-semibold">
              Cancel
            </button>
            <button
              onClick={onConfirm}
              className={`px-4 py-2 rounded-lg text-sm font-semibold text-white ${
                destructive ? 'bg-red-600 hover:bg-red-700' : 'bg-indigo-600 hover:bg-indigo-700'
              }`}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    );
  };

  const SkeletonRow = ({ cols }) => (
    <tr>
      {Array.from({ length: cols }).map((_, i) => (
        <td key={i} className="py-3">
          <div className="h-4 bg-slate-200 rounded animate-pulse" />
        </td>
      ))}
    </tr>
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
        className={`fixed top-6 right-6 flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border z-50
          animate-in slide-in-from-right-full fade-in duration-300 ${
          status.type === 'error'
            ? 'bg-red-50 border-red-100 text-red-700'
            : status.type === 'success'
              ? 'bg-emerald-50 border-emerald-100 text-emerald-700'
              : 'bg-white border-slate-100 text-slate-600'
        }`}
      >
        {status.type === 'error' && <AlertCircle size={18} />}
        {status.type === 'success' && <CheckCircle2 size={18} />}
        <span className="text-sm font-medium whitespace-pre-line">{status.message}</span>
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

  const handleAddPrompt = () => {
    setProfileForm((prev) => ({
      ...prev,
      aiPrompts: [...prev.aiPrompts, { prompt: '' }],
    }));
  };

  const handleRemovePrompt = (index) => {
    setProfileForm((prev) => ({
      ...prev,
      aiPrompts: prev.aiPrompts.filter((_, i) => i !== index),
    }));
  };

  const handleUpdatePrompt = (index, value) => {
    setProfileForm((prev) => {
      const nextPrompts = [...prev.aiPrompts];
      nextPrompts[index] = { ...nextPrompts[index], prompt: value };
      return { ...prev, aiPrompts: nextPrompts };
    });
  };

  const handleSaveProfile = async () => {
    if (!profileForm.phoneNumber) {
      logMessage('Phone number is required', 'error');
      return;
    }
    setSaving(true);
    try {
      await api.call('/user/update', {
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
          aiPrompts: profileForm.aiPrompts.map((entry) => ({
            prompt: entry.prompt,
          })),
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
        return fetch(buildApiUrl(apiBase, '/cv/upload'), {
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
      await api.call('/cv', { method: 'DELETE' });
      logMessage('CV deleted', 'success');
      setProfileForm((prev) => ({ ...prev, cvFileName: '' }));
    } catch (err) {
      logMessage(err.message, 'error');
    } finally {
      setCvDeleting(false);
    }
  };

  const handleDownloadCv = () => {
    const downloadOnce = async (authToken) => {
      const csrf = token ? await fetchCsrfToken() : null;
      return fetch(buildApiUrl(apiBase, '/cv/download'), {
        method: 'GET',
        credentials: 'include',
        headers: {
          ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
          ...(csrf ? { [csrf.headerName]: csrf.token } : {}),
        },
      });
    };

    const handleDownload = async () => {
      try {
        const resp = await downloadOnce(token);
        if (!resp.ok) {
          const parsed = await parseResponse(resp);
          if (parsed?.error === 'UNAUTHORIZED') {
            const newToken = await refreshAuthToken();
            const retryResp = await downloadOnce(newToken);
            if (!retryResp.ok) {
              const retryParsed = await parseResponse(retryResp);
              throw new Error((retryParsed && retryParsed.message) || retryParsed || 'CV download failed');
            }
            return retryResp;
          }
          throw new Error((parsed && parsed.message) || parsed || 'CV download failed');
        }
        return resp;
      } catch (err) {
        logMessage(err.message, 'error');
        return null;
      }
    };

    handleDownload().then(async (resp) => {
      if (!resp) return;
      const blob = await resp.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = profileForm.cvFileName || 'cv.pdf';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    });
  };

  const handleDeleteAccount = async () => {
    setDeletingAccount(true);
    try {
      await api.call('/user/delete', { method: 'DELETE' });
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
            <h2 className="text-xl font-extrabold text-slate-700 tracking-tight">Put AI to hunt jobs for you</h2>
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

              {/* Divider */}
              <div className="relative my-4">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-200"></div>
                </div>
                <div className="relative flex justify-center text-sm">
                  <span className="px-2 bg-white text-slate-400 font-medium">Or continue with</span>
                </div>
              </div>

              {/* Google Login Button */}
              <button
                type="button"
                onClick={() => window.location.href = '/oauth2/authorization/google'}
                className="w-full flex items-center justify-center gap-3 px-4 py-3.5 border border-slate-200 rounded-xl hover:bg-slate-50 transition-all font-semibold text-slate-700"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                <span>Continue with Google</span>
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
            className="w-10 h-10 mx-auto flex items-center justify-center rounded-xl text-slate-500 hover:bg-red-50 hover:text-red-600 transition-all"
            aria-label="Sign out"
          >
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col h-screen overflow-hidden">
        <div className="bg-white border-b border-slate-200">
          <div className="flex items-center gap-3 px-6 py-4">
            <div className="w-9 h-9 rounded-xl overflow-hidden shadow-lg shadow-indigo-100">
              <img src="/images/JobsHunterLogo.png" alt="JobsHunter logo" className="w-full h-full object-cover" />
            </div>
            <span className="font-black text-xl text-slate-900 tracking-tight">JobsHunter</span>
            
            {/* Breadcrumb indicator */}
            <div className="hidden lg:flex items-center gap-2 ml-4 text-slate-400">
              <span>/</span>
              <span className="text-sm font-medium text-indigo-600">
                {activeTab === 'profile' ? 'Profile' : 'Dashboard'}
              </span>
            </div>
          </div>
        </div>
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
              className="ml-2 h-10 w-10 flex items-center justify-center rounded-full bg-slate-100 text-slate-600"
              aria-label="Sign out"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto bg-slate-50/50 p-8 content-text-lg">
          {loading ? (
            <div className="h-full flex flex-col items-center justify-center text-slate-400 gap-4">
              <div className="relative">
                <div className="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin" />
              </div>
              <p className="font-bold text-sm tracking-wide">Fetching secure data...</p>
            </div>
          ) : (
            <div className="w-full max-w-none mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
              {activeTab === 'dash' && (
                <>
                  <section className="relative overflow-hidden bg-gradient-to-r from-white to-slate-200 rounded-[2rem] h-16 text-slate-700 shadow-xl shadow-slate-200">
                    <div className="relative z-10 flex items-center h-full px-6 gap-4">
                      <img
                        src={JobsHunterLogoSrc}
                        alt="JobsHunter logo"
                        className="h-12 w-12 object-cover rounded-xl"
                      />
                      <p className="flex-1 text-sm md:text-base font-medium leading-relaxed">
                        {bannerMessage}
                      </p>
                    </div>
                  </section>

                  <div className="space-y-8">
                    <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 space-y-4">
                      <p className="text-sm text-slate-500">
                        Hunt manually jobs using{' '}
                        <a
                          href="https://chatgpt.com/g/g-6965091020d08191be97e33b0c665349-jobshunter"
                          target="_blank"
                          rel="noreferrer"
                          className="text-indigo-600 font-semibold hover:underline"
                        >
                          JobsHunter ChatGPT model
                        </a>
                      </p>
                      <div className="flex items-center justify-between">
                        <h3 className="text-base font-bold text-slate-900">Orders</h3>
                        <button
                          onClick={() =>
                            logMessage(
                              'Due to cost constraints, this action is currently limited to the Product Owner.\nThank you for your understanding.',
                              'error'
                            )
                          }
                          className="px-4 py-2 rounded-full bg-indigo-700 text-white text-sm font-semibold hover:bg-indigo-800 transition-colors"
                        >
                          + New order
                        </button>
                      </div>
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm text-slate-700">
                          <thead>
                            <tr className="text-xs uppercase text-slate-400 border-b border-slate-100">
                              {orderColumns.map((col) => (
                                <th
                                  key={col.key}
                                  className={`relative py-2 font-semibold cursor-pointer ${
                                    col.align === 'right'
                                      ? 'text-right'
                                      : col.align === 'center'
                                        ? 'text-center'
                                        : 'text-left'
                                  }`}
                                  onClick={() => handleOrderSort(col.key)}
                                >
                                  <span className="inline-flex items-center gap-2 select-none">
                                    {col.label}
                                    <span className="text-xs text-slate-700 font-bold">{sortArrow(col.key)}</span>
                                  </span>
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {ordersLoading && (
                              <>
                                <SkeletonRow cols={8} />
                                <SkeletonRow cols={8} />
                                <SkeletonRow cols={8} />
                              </>
                            )}
                            {!ordersLoading && pagedOrders.length === 0 && (
                              <tr>
                                <td colSpan={8} className="py-12 text-center">
                                  <div className="flex flex-col items-center gap-3">
                                    <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center">
                                      <Globe className="w-8 h-8 text-slate-400" />
                                    </div>
                                    <p className="text-slate-500 font-medium">No orders yet</p>
                                    <p className="text-sm text-slate-400">Start a new order to begin hunting for jobs</p>
                                  </div>
                                </td>
                              </tr>
                            )}
                            {!ordersLoading &&
                              pagedOrders.map((order) => (
                                <tr
                                  key={order.id}
                                  onClick={() => {
                                    setSelectedOrderId(order.id);
                                  }}
                                  className={`border-b border-slate-100 last:border-b-0 cursor-pointer hover:bg-slate-50 ${
                                    selectedOrderId === order.id ? 'bg-indigo-100/70' : ''
                                  }`}
                                >
                                  <td className="py-2 text-slate-500">
                                    {order.modifiedAt ? new Date(order.modifiedAt).toLocaleString() : '-'}
                                  </td>
                                  <td className="py-2 font-medium text-slate-900">{order.provider}</td>
                                  <td className="py-2">{order.model}</td>
                                  <td className="py-2 text-center">
                                    <input type="checkbox" checked={order.searchCompanies} readOnly />
                                  </td>
                                  <td className="py-2 text-center">
                                    <input type="checkbox" checked={order.searchByPrompts} readOnly />
                                  </td>
                                  <td className="py-2 text-left">
                                    <span
                                      className={`px-3 py-1 rounded-full text-xs font-semibold inline-flex items-center justify-center gap-1.5 min-w-[110px] ${
                                        order.status === 'COMPLETED'
                                          ? 'bg-emerald-100 text-emerald-700'
                                          : order.status === 'FAILED'
                                            ? 'bg-red-100 text-red-700'
                                            : 'bg-amber-100 text-amber-700'
                                      }`}
                                    >
                                      {order.status === 'COMPLETED' && <span>✓</span>}
                                      {order.status === 'FAILED' && <span>✕</span>}
                                      {(order.status === 'PROCESSING' || order.status === 'NEW') && <span>⏳</span>}
                                      {order.status}
                                    </span>
                                  </td>
                                  <td className="py-2 text-left text-slate-600">{order.jobsFound ?? 0}</td>
                                  <td
                                    className="py-2 text-left text-slate-500 truncate max-w-[200px]"
                                    title={order.errorMessage || ''}
                                  >
                                    {order.errorMessage || '-'}
                                  </td>
                                </tr>
                              ))}
                          </tbody>
                        </table>
                      </div>
                      {totalOrderPages > 1 && (
                        <div className="flex items-center justify-between text-sm text-slate-500">
                          <span>
                            Page {ordersPage} of {totalOrderPages}
                          </span>
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => setOrdersPage((prev) => Math.max(1, prev - 1))}
                              disabled={ordersPage === 1}
                              className="px-4 py-2 min-h-[44px] min-w-[44px] rounded-full border border-slate-200 disabled:opacity-50 flex items-center justify-center"
                            >
                              Prev
                            </button>
                            <button
                              onClick={() => setOrdersPage((prev) => Math.min(totalOrderPages, prev + 1))}
                              disabled={ordersPage === totalOrderPages}
                              className="px-4 py-2 min-h-[44px] min-w-[44px] rounded-full border border-slate-200 disabled:opacity-50 flex items-center justify-center"
                            >
                              Next
                            </button>
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 space-y-4">
                      <div className="sticky top-4 bg-white z-10 pb-3">
                        <div className="flex flex-wrap items-center gap-4 text-sm text-slate-600">
                          <span className="font-semibold text-slate-900">Selected order</span>
                          <span>{selectedOrder?.provider || '-'}</span>
                          <span>{selectedOrder?.model || '-'}</span>
                          <span>{selectedOrder?.modifiedAt ? new Date(selectedOrder.modifiedAt).toLocaleString() : '-'}</span>
                          <span>
                            {selectedOrder
                              ? [selectedOrder.searchCompanies ? 'By company' : null, selectedOrder.searchByPrompts ? 'By prompts' : null]
                                  .filter(Boolean)
                                  .join(' / ')
                              : '-'}
                          </span>
                          <span className="flex items-center gap-2">
                            {selectedOrder ? (
                              <span
                                className={`px-3 py-1 rounded-full text-xs font-semibold inline-flex justify-center min-w-[110px] ${
                                  selectedOrder.status === 'COMPLETED'
                                    ? 'bg-emerald-100 text-emerald-700'
                                    : selectedOrder.status === 'FAILED'
                                      ? 'bg-red-100 text-red-700'
                                      : 'bg-amber-100 text-amber-700'
                                }`}
                              >
                                {selectedOrder.status}
                              </span>
                            ) : (
                              '-'
                            )}
                          </span>
                          <span>{selectedOrder?.jobsFound ?? 0} jobs</span>
                        </div>
                      </div>
                      <h3 className="text-base font-bold text-slate-900">Jobs found</h3>
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm text-slate-700">
                          <thead>
                            <tr className="text-xs uppercase text-slate-400 border-b border-slate-100">
                              <th className="text-left py-2 font-semibold">#</th>
                              <th
                                className="text-left py-2 font-semibold cursor-pointer"
                                onClick={() => handleJobsSort('url')}
                              >
                                URL <span className="text-xs text-slate-700 font-bold">{jobsSortArrow('url')}</span>
                              </th>
                              <th
                                className="text-left py-2 font-semibold cursor-pointer"
                                onClick={() => handleJobsSort('createdAt')}
                              >
                                Date <span className="text-xs text-slate-700 font-bold">{jobsSortArrow('createdAt')}</span>
                              </th>
                              <th
                                className="text-left py-2 font-semibold cursor-pointer"
                                onClick={() => handleJobsSort('engine')}
                              >
                                Engine <span className="text-xs text-slate-700 font-bold">{jobsSortArrow('engine')}</span>
                              </th>
                              <th
                                className="text-left py-2 font-semibold cursor-pointer"
                                onClick={() => handleJobsSort('model')}
                              >
                                Model <span className="text-xs text-slate-700 font-bold">{jobsSortArrow('model')}</span>
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            {jobsLoading && (
                              <>
                                <SkeletonRow cols={5} />
                                <SkeletonRow cols={5} />
                                <SkeletonRow cols={5} />
                              </>
                            )}
                            {!jobsLoading && jobsFound.length === 0 && (
                              <tr>
                                <td colSpan={5} className="py-12 text-center">
                                  <div className="flex flex-col items-center gap-3">
                                    <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center">
                                      <AlertCircle className="w-8 h-8 text-slate-400" />
                                    </div>
                                    <p className="text-slate-500 font-medium">
                                      {selectedOrderId ? 'No jobs found' : 'No order selected'}
                                    </p>
                                    <p className="text-sm text-slate-400">
                                      {selectedOrderId ? 'This order has not found any matching jobs yet' : 'Select an order above to view its jobs'}
                                    </p>
                                  </div>
                                </td>
                              </tr>
                            )}
                            {!jobsLoading &&
                              sortedJobs.map((job, index) => (
                                <tr key={`${job.url}-${index}`} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                                  <td className="py-3 text-slate-500">{index + 1}</td>
                                  <td className="py-3">
                                    <a className="text-indigo-600 hover:underline" href={job.url} target="_blank" rel="noreferrer">
                                      {job.url}
                                    </a>
                                  </td>
                                  <td className="py-3 text-slate-500">
                                    {job.createdAt ? new Date(job.createdAt).toLocaleString() : '-'}
                                  </td>
                                  <td className="py-3">{selectedOrder?.provider || '-'}</td>
                                  <td className="py-3">{selectedOrder?.model || '-'}</td>
                                </tr>
                              ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
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

                            <div className="hidden lg:block w-1 bg-slate-400 h-full rounded-full" />

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
                                    onClick={() => setConfirmDeleteCv(true)}
                                    disabled={cvDeleting}
                                    className="px-4 py-2 rounded-lg bg-red-100 text-red-700 text-sm font-semibold disabled:opacity-70"
                                  >
                                    {cvDeleting ? 'Deleting...' : 'Delete'}
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>

                          <div className="h-1 bg-slate-400 rounded-full my-4" />

                          <div className="space-y-3">
                            <h3 className="text-lg font-semibold text-slate-900">Location</h3>
                            <div className="grid sm:grid-cols-2 gap-4">
                              <div>
                                <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">Country</p>
                                <select
                                  value={profileForm.country || ''}
                                  onChange={(e) => handleProfileChange('country', e.target.value)}
                                  className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                  disabled={countriesLoading}
                                >
                                  <option value="" disabled>
                                    {countriesLoading ? 'Loading countries...' : 'Select country'}
                                  </option>
                                  {countries.map((country) => (
                                    <option key={country} value={country}>
                                      {country}
                                    </option>
                                  ))}
                                </select>
                              </div>
                              <div>
                                <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-1">City</p>
                                <input
                                  value={profileForm.city}
                                  onChange={(e) => handleProfileChange('city', e.target.value)}
                                  className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                  placeholder="Select city"
                                />
                              </div>
                            </div>
                          </div>

                      <div className="h-1 bg-slate-400 rounded-full my-4" />

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

                            <div>
                              <p className="text-xs font-bold text-slate-500 uppercase tracking-wide mb-2">Open to Relocation</p>
                              <select
                                value={profileForm.relocation || ''}
                                onChange={(e) => handleProfileChange('relocation', e.target.value || null)}
                                className="w-full px-3 py-2 rounded-lg bg-white border border-slate-200 text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                              >
                                <option value="">Select relocation preference</option>
                                {relocationOptions.map((opt) => (
                                  <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                  </option>
                                ))}
                              </select>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className="h-1 bg-slate-400 rounded-full my-4" />

                      <div className="space-y-3">
                        <h3 className="text-lg font-semibold text-slate-900">Prompts</h3>
                        <div className="border border-slate-200 rounded-2xl bg-slate-50">
                          <div className="p-4 space-y-3">
                            {profileForm.aiPrompts.length === 0 && (
                              <p className="text-sm text-slate-500">No prompts added yet.</p>
                            )}
                            {profileForm.aiPrompts.map((entry, index) => (
                              <div
                                key={`prompt-${index}`}
                                className="flex flex-col gap-2 text-sm text-slate-800 bg-white border border-slate-200 rounded-xl p-3"
                              >
                                <textarea
                                  rows={3}
                                  value={entry.prompt}
                                  onChange={(e) => handleUpdatePrompt(index, e.target.value)}
                                  className="w-full text-sm outline-none resize-none leading-relaxed max-h-32 overflow-y-auto"
                                  placeholder="Write your prompt..."
                                />
                                <div className="flex justify-end">
                                  <button
                                    onClick={() => handleRemovePrompt(index)}
                                    className="text-indigo-600 font-semibold text-xs hover:underline"
                                  >
                                    Delete
                                  </button>
                                </div>
                              </div>
                            ))}
                            <button
                              onClick={handleAddPrompt}
                              className="px-4 py-2 rounded-lg bg-indigo-700 text-white text-sm font-semibold"
                            >
                              + Add prompt
                            </button>
                          </div>
                        </div>
                      </div>

                      <div className="h-1 bg-slate-400 rounded-full my-4" />

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

                      <div className="h-1 bg-slate-400 rounded-full my-4" />

                      <div className="flex flex-wrap items-center justify-between gap-4 pt-2">
                        <button
                          onClick={handleSaveProfile}
                          disabled={saving}
                          className="w-64 px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-lg shadow-indigo-200 transition-all duration-200 disabled:opacity-70 flex items-center justify-center gap-2"
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
                          onClick={() => setConfirmDeleteAccount(true)}
                          disabled={deletingAccount}
                          className="px-4 py-2.5 rounded-xl border-2 border-red-200 bg-red-50 hover:bg-red-100 hover:border-red-300 text-red-600 text-sm font-semibold transition-all duration-200 disabled:opacity-70 flex items-center gap-2"
                        >
                          <Trash2 size={16} />
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

      <ConfirmDialog
        open={confirmDeleteCv}
        title="Delete CV"
        message="Are you sure you want to delete your CV? This action cannot be undone."
        confirmLabel="Delete"
        destructive
        onCancel={() => setConfirmDeleteCv(false)}
        onConfirm={() => { setConfirmDeleteCv(false); handleDeleteCv(); }}
      />

      <ConfirmDialog
        open={confirmDeleteAccount}
        title="Delete Account"
        message="This will permanently delete your account and all associated data. This cannot be undone."
        confirmLabel="Delete Account"
        destructive
        onCancel={() => setConfirmDeleteAccount(false)}
        onConfirm={() => { setConfirmDeleteAccount(false); handleDeleteAccount(); }}
      />

      <StatusToast />
    </div>
  );
};

export default App;
