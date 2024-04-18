var cur = db.profiles.find();
while (cur.hasNext()) {
	var profile = cur.next();

	var locus = {
		1: ["D1S1656","D2S1338","TPOX","D2S441","F13B","D2S1360","D3S1358","D3S1744","FGA","GABA","D4S2366","D5S818","CSF1PO","D5S2500","F13A","ACTBP2","D6S1043","D6S474","D7S820","D7S1517","D8S1179","LPL","D8S1132","PentaC","D10S1248","D10S2325","TH01","vWA","CD4","D12S391","D13S317","Noloci14","FESFPS","PentaE","D16S539","Noloci17","D18S51","D19S433","Noloci20","D21S11","PentaD","D21S2055","D22S1045","YIndel"],
		2: ["AMEL","DXS8378","DXS10134","HPRTB","DXS10148","DXS10135","DXS10103","DXS10101","DXS10146","DXS7132","DXS10079","DXS10074","DXS7423"],
		3: ["DYS19","DYS456","DYS389II","DYS390","DYS635","DYS391","DYS385ab","DYS389I","DYS393","DYS458","DYS439","DYS385","DYS533","DYS570","DYS448","DYS437","DYS549","DYS481","DYS576","DYS392","DYS438","YGATAH4","DYS643"],
		4: ["MT"]
	};

	var genotypification = profile.genotypification;

	var genotypificationByType = {};

    var update = true;

	for (var i = 1; i < 5; i++) {
        if (genotypification[i] != null) { update = false; break; }
        var aux = {};
        locus[i].forEach(function (marker) {
            if (genotypification[marker] != null) {
                aux[marker] = genotypification[marker];
            }
        });
        if (Object.keys(aux).length > 0) {
            genotypificationByType[i] = aux;
        }
	}

    if (update) db.profiles.update({_id:profile._id}, {$set: {genotypification: genotypificationByType}});

}