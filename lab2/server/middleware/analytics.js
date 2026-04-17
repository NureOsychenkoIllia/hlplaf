const { analyticsEvents, uuidv4 } = require('../data/store');

const trackEvent = (event) => (req, res, next) => {
  analyticsEvents.push({
    id: uuidv4(),
    trackId: req.params.id,
    userId: req.headers['x-user-id'] || null,
    event,
    ts: new Date().toISOString(),
  });
  next();
};

module.exports = { trackEvent };
