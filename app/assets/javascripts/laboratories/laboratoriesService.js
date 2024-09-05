define([], function() {
'use strict';

	function LaboratoriesService(playRoutes, $http) {

		this.getLaboratory = function(code){
			console.log('GET LABORATORY');
			return $http.get('/laboratory/' + encodeURIComponent(code));
			//return playRoutes.controllers.Laboratories.getLaboratory(code).get();
		};

		this.getLaboratories = function(){	
			console.log('GET LABORATORIES');
			return $http.get('/laboratory');
			//return playRoutes.controllers.Laboratories.list().get();
		};
					
		this.getLaboratoriesDescriptive = function(){
			console.log('GET LABORATORIES DESCRIPTIVE');
			return $http.get('/laboratory/descriptive');
			//return playRoutes.controllers.Laboratories.listDescriptive().get();
		};
					
		this.getCountries = function(){
			console.log('GET COUNTRIES');
			return $http.get('/country');			
			//return playRoutes.controllers.Laboratories.listCountries().get();
		};
		
		this.getProvinces = function(country){
			console.log('GET PROVINCES');
			return $http.get('/provinces/' + country);
			//return playRoutes.controllers.Laboratories.listProvinces(country).get();
		};
		
		this.createLaboratory = function(laboratory){
			console.log('ADD LABORATORY');
			return $http.post('/laboratory', laboratory);
			//return playRoutes.controllers.Laboratories.addLab().post(laboratory);
		};

		this.updateLaboratory = function(laboratory){
			console.log('UPDATE LABORATORY');
			return $http.put('/laboratory', laboratory);
			//return playRoutes.controllers.Laboratories.updateLab().put(laboratory);
		};
	}

	return LaboratoriesService;
});