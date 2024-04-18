define([], function() {
'use strict';

function enablePermissions(userService) {
	return {
		restrict : 'A',
		link: function(scope, el, attrs) {
			el.attr('disabled', 'disabled');
			userService.onLogin(function(user) {
				var enable = user.permissions.indexOf(attrs.enablePermissions) > -1;
				if(enable) {
					el.removeAttr('disabled');
				} else {
					el.attr('disabled', 'disabled');
				}
			});
		}
	};
}

return enablePermissions;

});
