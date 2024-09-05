define([], function() {
'use strict';

	function BioMaterialTypeService(playRoutes,$http) {
		
		this.getBioMaterialTypes = function() {
			console.log('GET BIO MATERIAL TYPES');
			return $http.get('/bioMaterialTypes');
			//return playRoutes.controllers.BioMaterialTypes.list().get();
		};
		
		this.addBioMaterialType = function(bmt) {
			console.log('ADD BIO MATERIAL TYPES');
			return $http.post('/bioMaterialTypes', bmt);			
			//return playRoutes.controllers.BioMaterialTypes.insert().post(bmt);
		};
		
		this.updateBioMaterialType = function(bmt) {
			console.log('UPDATE BIO MATERIAL TYPES');
			return $http.put('/bioMaterialTypes', bmt);
			//return playRoutes.controllers.BioMaterialTypes.update().put(bmt);
		};
		
		this.deleteBioMaterialType = function(id) {
			console.log('DELETE BIO MATERIAL TYPES');
			return $http.delete('/bioMaterialTypes/' + id);
			//return playRoutes.controllers.BioMaterialTypes.remove(id).delete();
		};
	}

	return BioMaterialTypeService;
});