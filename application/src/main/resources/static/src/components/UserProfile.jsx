// [Issue #46] Standalone component for managing user language preferences.
// Allows users to add/remove spoken languages from their profile.
// These languages are used by the backend LanguageMatchRule to filter job postings.
import React, { useState, useEffect } from 'react';
import { Globe } from 'lucide-react';

const UserProfile = () => {
  const [user, setUser] = useState({ username: '', email: '', languages: [] });
  // [Issue #46] Predefined list of languages matching the seeded DB values
  const [availableLanguages, setAvailableLanguages] = useState(['English', 'French', 'Romanian', 'Spanish', 'German', 'Italian']);
  const [selectedLanguage, setSelectedLanguage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    // Fetch user data on component mount
    fetch('/api/user')
      .then(response => response.json())
      .then(data => {
        setUser(data);
        setIsLoading(false);
      })
      .catch(error => {
        console.error('Error fetching user data:', error);
        setMessage('Error loading profile.');
        setIsLoading(false);
      });
  }, []);

  // [Issue #46] Sends POST /api/user/languages to associate a new language with the user
  const handleAddLanguage = () => {
    if (selectedLanguage && !user.languages.includes(selectedLanguage)) {
      fetch('/api/user/languages', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ language: selectedLanguage }),
      })
        .then(response => response.json())
        .then(updatedUser => {
          setUser(updatedUser);
          setSelectedLanguage('');
          setMessage('Language added successfully.');
        })
        .catch(error => {
          console.error('Error adding language:', error);
          setMessage('Error adding language.');
        });
    } else if (user.languages.includes(selectedLanguage)) {
      setMessage('Language already added.');
    }
  };

  // [Issue #46] Sends DELETE /api/user/languages/{language} to remove a language from the user
  const handleRemoveLanguage = (language) => {
    fetch(`/api/user/languages/${language}`, {
      method: 'DELETE',
    })
      .then(response => response.json())
      .then(updatedUser => {
        setUser(updatedUser);
        setMessage('Language removed successfully.');
      })
      .catch(error => {
        console.error('Error removing language:', error);
        setMessage('Error removing language.');
      });
  };

  if (isLoading) {
    return <div className="text-center p-4">Loading profile...</div>;
  }

  return (
    <div className="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-md">
      <h2 className="text-2xl font-bold mb-4">User Profile</h2>
      <div className="mb-4">
        <p className="text-lg"><strong>Username:</strong> {user.username}</p>
        <p className="text-lg"><strong>Email:</strong> {user.email}</p>
      </div>
      <div className="mb-4">
        <h3 className="text-xl font-semibold mb-2 flex items-center">
          <Globe className="mr-2" size={20} />
          Languages
        </h3>
        <ul className="list-disc pl-5">
          {user.languages.length > 0 ? (
            user.languages.map((lang, index) => (
              <li key={index} className="flex justify-between items-center p-1 hover:bg-gray-100 rounded">
                {lang}
                <button 
                  onClick={() => handleRemoveLanguage(lang)} 
                  className="text-red-500 hover:text-red-700 ml-2 text-sm"
                >
                  Remove
                </button>
              </li>
            ))
          ) : (
            <li className="text-gray-500">No languages added yet.</li>
          )}
        </ul>
      </div>
      <div className="mb-4 flex gap-2">
        <select 
          value={selectedLanguage} 
          onChange={(e) => setSelectedLanguage(e.target.value)}
          className="border border-gray-300 rounded p-2 flex-1"
        >
          <option value="">Select a language</option>
          {availableLanguages
            .filter(lang => !user.languages.includes(lang))
            .map((lang, index) => (
              <option key={index} value={lang}>{lang}</option>
            ))
          }
        </select>
        <button 
          onClick={handleAddLanguage} 
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
          disabled={!selectedLanguage}
        >
          Add
        </button>
      </div>
      {message && (
        <div className={`text-center p-2 rounded ${message.includes('Error') ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`}>
          {message}
        </div>
      )}
    </div>
  );
};

export default UserProfile;
