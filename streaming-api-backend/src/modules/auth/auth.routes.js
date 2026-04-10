const router = require('express').Router();
const auth   = require('../../middleware/auth');
const {
  registerHandler,
  loginHandler,
  refreshHandler,
  logoutHandler,
} = require('./auth.controller');

router.post('/register', registerHandler);
router.post('/login',    loginHandler);
router.post('/refresh',  refreshHandler);
router.post('/logout',   auth, logoutHandler); // requiere token válido

module.exports = router;