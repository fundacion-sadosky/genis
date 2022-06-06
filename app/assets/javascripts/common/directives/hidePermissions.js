define([], function() {
'use strict';

function hidePermissions(userService) {
	return {
		restrict : 'A',
		link: function(scope, el, attrs) {
			el.hide();
			userService.onLogin(function() {
				var show = userService.hasAnyPermission(attrs.hidePermissions);
				el.toggle(!show);
			});
		}
	};
}

return hidePermissions;

});
