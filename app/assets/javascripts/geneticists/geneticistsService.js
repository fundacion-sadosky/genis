define([], function() {
'use strict';

	function GeneticistsService(playRoutes, $http) {
		
		this.getGeneticists = function(lab) {
			console.log('ALL GENETICISTS');
			return $http.get('/geneticist/' + encodeURIComponent(lab));
			//return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
		};
		
		this.getGeneticist = function(id) {
			console.log('GET GENETICISTS');
			return $http.get('/geneticist', {params: { geneticistId: id }});
			//return playRoutes.controllers.Geneticists.getGeneticist(id).get();
		};
		
		this.updateGeneticist = function(gen) {
			console.log('UPDATE GENETICISTS');
			return $http.put('/geneticist', gen);
			//return playRoutes.controllers.Geneticists.updateGeneticist().put(gen);
		};
		
		this.saveGeneticist = function(gen){
			console.log('ADD GENETICISTS');
			return  $http.post('/geneticist', gen);
			//return playRoutes.controllers.Geneticists.addGeneticist().post(gen);
		};
		
	}

	return GeneticistsService;
});