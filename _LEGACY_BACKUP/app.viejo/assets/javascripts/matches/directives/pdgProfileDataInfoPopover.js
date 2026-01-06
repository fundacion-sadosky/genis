define([ 'jquery' ],
function($) {
'use strict';

function pdgProfileDataInfoPopover(profileDataService, matchesService) {
	return {
		restrict : 'A',
		scope : {
			// dataprofile: '=',
			profileid : '='
		},
		templateUrl : '/assets/javascripts/matches/directives/pdg-profile-data-info-popover.html',
		link : function(scope, el, attrs) {
			
			attrs.$observe('info', function(value) {
				if (value) {
					scope.profileData = JSON.parse(value);
					
					profileDataService.getCategories().then(function(response){
						var categories = response.data;
                        scope.subcatName = matchesService.getSubCatName(categories, scope.profileData.category);
					});

					el.popover({
						html : true,
						content : function() {
							return $('#popoverProfiledataInfo_' + scope.profileid).html();
						},
						//trigger: 'focus',
						placement : attrs.popoverPlacement || 'right'
					});
				}
			});

			el.on('$destroy', function(/* event */) {
				el.popover('destroy');
			});
		}
	};
}

return pdgProfileDataInfoPopover;

});