define([], function() {
'use strict';

function CategoriesService($q, playRoutes) {

	this.getCategories = function() {
		var groupsPromise = playRoutes.controllers.Categories.categoryTree().get().then(function(response) {
			var groups = response.data;
			for ( var grpId in groups) {
				var group = groups[grpId];
				var catIds = [];
				for (var i = 0; i < group.subcategories.length; i++) {
					catIds.push(group.subcategories[i].id);
				}
				group.categories = catIds;
				group.pedigreeAssociation = false;
				delete group.subcategories;
			}
			return groups;
		});

		var categoriesPromise = playRoutes.controllers.Categories.list().get().then(function(response) {
			return response.data;
		});
		
		return $q.all({
			groups: groupsPromise,
			categories: categoriesPromise
		});

	};

	this.createCategory = function(category) {
		return playRoutes.controllers.Categories.addCategory().post(category);
	};

	this.updateCategory = function(category) {
		return playRoutes.controllers.Categories.updateCategory(category.id).put(category);
	};

	this.updateFullCategory = function(category) {
		return playRoutes.controllers.Categories.updateFullCategory(category.id).put(category);
	};

	this.removeCategory = function(category) {
		return playRoutes.controllers.Categories.removeCategory(category.id).delete();
	};

	this.createGroup = function(group) {
		return playRoutes.controllers.Categories.addGroup().post(group);
	};

	this.updateGroup = function(group) {
		return playRoutes.controllers.Categories.updateGroup(group.id).put(group);
	};

	this.removeGroup = function(group) {
		return playRoutes.controllers.Categories.removeGroup(group.id).delete();
	};
}
	
return CategoriesService;

});