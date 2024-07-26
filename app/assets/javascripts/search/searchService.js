// acá intento pasar un route a llamado de api
define([], function() {
	'use strict';

	function SearchService(playRoutes, userService){

		this.search = function(search) {
			var user = userService.getUser();
			search.userId = user.name;
			search.isSuperUser = user.superuser;

			/*try {
				var response = fetch('/search/profileData/search', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(search)
				});

				if (!response.ok) {
					throw new Error('Error in request: ' + response.statusText);
				}

				return response.json();
			} catch (error) {
				console.error('Error during search:', error);
				throw error;
			}*/

			return playRoutes.controllers.SearchProfileDatas.search().post(search);
		};

		this.searchTotal = function(search) {
			var user = userService.getUser();
			search.userId = user.name;
			search.isSuperUser = user.superuser;
			return playRoutes.controllers.SearchProfileDatas.searchTotal().post(search);
		};

		this.searchMatches = function(filters) {
			return playRoutes.controllers.SearchMatches.search().post(filters);
		};

		this.searchProfilesAssociable = function(input,category){
			return playRoutes.controllers.SearchProfileDatas.searchProfilesAssociable(input, category).get();
		};
		this.getMotives = function()  {
			var motiveTypeReject =  2;
			return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
		};

		this.getCategories = function()  {
			return playRoutes.controllers.Categories.categoryTree().get();
		};

		this.getCategoriesWithProfiles = function(){
			return playRoutes.controllers.Categories.listWithProfiles().get();
		};

	}

	return SearchService;
});