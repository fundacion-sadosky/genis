define(['angular', './signupController', './clearPassController', 'users', 'common'],
function(angular, signupController,clearPassController) {
'use strict';

angular
	.module('pdg.signup', ['pdg.users', 'pdg.common'])
	.controller('signupController', [ '$scope', 'userService', 'roleService', '$modal', 'alertService', signupController])
    .controller('clearPassController', [ '$scope', 'userService', 'roleService', '$modal', 'alertService', clearPassController]
);

return undefined;

});
