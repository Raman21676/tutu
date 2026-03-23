# Privacy Policy Hub - Quick Start Guide

## 📁 Folder Structure

```
privacy/
├── index.html              # Main hub page (lists all your apps)
├── TEMPLATE.html          # Template for new apps
├── tutu-browser/          # App 1: TuTu Browser
│   └── index.html
├── app2/                  # App 2 (create when needed)
│   └── index.html
├── app3/                  # App 3 (create when needed)
│   └── index.html
└── README.md              # This file
```

## 🚀 How to Add a New App Privacy Policy

### Step 1: Copy Template
```bash
cp TEMPLATE.html app-name/index.html
```

### Step 2: Edit the New Policy
Replace these placeholders in the new `index.html`:

| Placeholder | Replace With |
|-------------|--------------|
| `APP_NAME` | Your app's name (e.g., "My Cool App") |
| `APP_TAGLINE` | Short description (e.g., "Best Calculator Ever") |
| `DATE` | Today's date (e.g., "March 23, 2026") |
| `your-email@example.com` | Your actual email |
| `[Purpose 1]`, `[Purpose 2]` | What data the app stores locally |

### Step 3: Add to Hub Index
Edit `index.html` and add a new app card in the `.app-list` section:

```html
<a href="app-name/" class="app-card">
    <div class="app-icon">📱</div>
    <div class="app-info">
        <h3>App Name</h3>
        <p>Short description of the app</p>
    </div>
    <div class="app-arrow">→</div>
</a>
```

### Step 4: Deploy to GitHub Pages
```bash
cd ~/Desktop/TuTu-final/privacy-hub
git init
git remote add origin git@github.com:Raman21676/android.git
git add .
git commit -m "Add privacy policy for [App Name]"
git push -f origin main
```

## 🔗 URL Structure

Once deployed to GitHub Pages at `1click.live`:

| Page | URL |
|------|-----|
| Privacy Hub | `https://1click.live/privacy/` |
| TuTu Browser | `https://1click.live/privacy/tutu-browser/` |
| App 2 | `https://1click.live/privacy/app2/` |
| App 3 | `https://1click.live/privacy/app3/` |

## ✅ Pre-Publish Checklist

Before publishing each app:

- [ ] Privacy policy HTML created from template
- [ ] App name updated in title and content
- [ ] Email address updated
- [ ] Date updated
- [ ] Data collection points customized for the app
- [ ] Added to hub index.html
- [ ] Tested locally (open in browser)
- [ ] Pushed to GitHub
- [ ] URL tested on live site

## 🎨 Customizing Colors

Each app can have its own color scheme. Edit the CSS in each `index.html`:

```css
/* Change these colors to match your app's branding */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);  /* Page background */
color: #667eea;  /* Headings */
border-left: 4px solid #667eea;  /* Highlight boxes */
```

## 📱 Icon Reference

Use emojis for app icons in the hub:

| App Type | Icon |
|----------|------|
| Browser | 🌐 |
| Calculator | 🧮 |
| Calendar | 📅 |
| Camera | 📷 |
| Chat/Messaging | 💬 |
| Clock/Alarm | ⏰ |
| File Manager | 📁 |
| Fitness | 💪 |
| Gallery | 🖼️ |
| Game | 🎮 |
| Music | 🎵 |
| Notes | 📝 |
| Settings | ⚙️ |
| Weather | 🌤️ |
| Generic | 📱 |

## 🆘 Need Help?

1. **Test locally**: Open the HTML file in your browser
2. **Check console**: Press F12 for any errors
3. **Verify links**: Make sure all relative links work
4. **Mobile test**: Check on your phone

## 📝 Example: Adding App #2

```bash
# 1. Create folder
mkdir ~/Desktop/TuTu-final/privacy-hub/my-calculator

# 2. Copy template
cp ~/Desktop/TuTu-final/privacy-hub/TEMPLATE.html \
   ~/Desktop/TuTu-final/privacy-hub/my-calculator/index.html

# 3. Edit the file (use any editor)
# Replace APP_NAME with "My Calculator"
# Replace APP_TAGLINE with "Simple & Fast Calculator"
# Update email, date, and data collection details

# 4. Add to hub index.html (follow Step 3 above)

# 5. Deploy
cd ~/Desktop/TuTu-final/privacy-hub
git add .
git commit -m "Add My Calculator privacy policy"
git push origin main
```

**Your privacy policy URL:** `https://1click.live/privacy/my-calculator/`
