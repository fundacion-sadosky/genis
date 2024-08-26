define(['angular', './loginController', 'users', 'signup', 'common'], 
function(angular, loginController) {
'use strict';

angular
	.module('pdg.login', ['pdg.users', 'pdg.signup', 'pdg.common'])
	.controller('loginController', [ '$scope', '$log', '$location', 'userService', loginController]);

return undefined;

});
