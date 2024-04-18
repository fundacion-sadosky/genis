var cur = db.pedigrees.find();
while (cur.hasNext()) {
	var pedigree = cur.next();
    db.pedigrees.remove({_id:pedigree._id});
    pedigree._id = pedigree._id.toString();
    db.pedigrees.insert(pedigree);
}