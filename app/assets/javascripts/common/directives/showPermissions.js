define([], function() {
'use strict';

function showPermissions(userService) {
	return {
		restrict : 'A',
		link: function(scope, el, attrs) {
			el.hide();
			userService.onLogin(function() {
				var show = userService.hasAnyPermission(attrs.showPermissions);
				el.toggle(show);
			});
		}
	};
}

return showPermissions;

});
