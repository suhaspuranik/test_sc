import React, { useState } from 'react';
import './login.css';
import { Eye, EyeOff, User, Lock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Login = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const togglePassword = () => setShowPassword((prev) => !prev);

  const handleLogin = (e) => {
    e.preventDefault();

    if (username === 'dataprowess' && password === 'root') {
      navigate('/dashboard');
    } else {
      alert('Invalid username or password!');
    }
  };

  return (
    <div className="login-page">
      <div className="background-image"></div>

      <div className="login-container large-container">
        <div className="image-side">
          <img src="/images/elephants_guards_final.png" alt="forestguard" className="tree-image" />
        </div>

        <div className="login-box">
          <h2>Login</h2>

          <form onSubmit={handleLogin}>
            <div className="input-group">
              <label htmlFor="username">Username</label>
              <div className="input-icon-wrapper">
                <User size={18} className="input-icon" />
                <input
                  id="username"
                  type="text"
                  placeholder="Enter your username"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
            </div>

            <div className="input-group">
              <label htmlFor="password">Password</label>
              <div className="input-icon-wrapper">
                <Lock size={18} className="input-icon" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <span
                  onClick={togglePassword}
                  className="eye-toggle"
                  title="Toggle password visibility"
                >
                  {showPassword ? <Eye size={18} /> : <EyeOff size={18} />}
                </span>
              </div>
            </div>

            <div className="forgot-password">
              <a href="#">Forgot Password?</a>
            </div>

            <button type="submit" className="login-btn">LOGIN</button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Login;