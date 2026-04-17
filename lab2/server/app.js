const express     = require('express');
const cors        = require('cors');
const compression = require('compression');
const path        = require('path');

const app = express();

app.use(compression());
app.use(cors());
app.use(express.json());
app.use('/static', express.static(path.join(__dirname, 'static')));

app.use('/api/tracks',    require('./routes/tracks'));
app.use('/api/playlists', require('./routes/playlists'));
app.use('/api/auth',      require('./routes/users'));
app.use('/api/users',     require('./routes/users'));
app.use('/api/reviews',   require('./routes/reviews'));
app.use('/api/analytics', require('./routes/analytics'));

const clientDist = path.join(__dirname, '../client/dist');
app.use(express.static(clientDist));
app.get('*', (req, res) => res.sendFile(path.join(clientDist, 'index.html')));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server listening on http://localhost:${PORT}`));
