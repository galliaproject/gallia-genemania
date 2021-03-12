<p align="center"><img src="./images/logo.png" alt="icon"></p>

<ins>__Note__</ins>: _This is only intended to showcase processing in Gallia, it is not complete nor thoroughly tested at the moment. Use output at your own risk._

For more information, see gallia-core [documentation](https://github.com/galliaproject/gallia-core/blob/init/README.md#introducing-gallia-a-scala-library-for-data-manipulation), in particular the bioinformatics examples [section](https://github.com/galliaproject/gallia-core/blob/init/README.md#bioinformatics-examples).

<a name="description"></a>
### Description
Uses _Gallia_ [transformations](https://github.com/galliaproject/gallia-genemania/blob/init/src/main/scala/galliaexample/genemania/GeneMania.scala#L50) to turn TSV data from <http://genemania.org/data/current/Homo_sapiens/> into objects like:

<a name="output"></a>
```
{
  "_id": "ENSG00000123456",

  "co_expression": [
    { "target": "ENSG00000654321",
      "context": [
        { "weight": 0.021,
          "network": "Meier-Seiler-2009",
          "source": "GEO",
          "pubmed": "12345678" },
        { "weight": 0.019,. .. },
        ...
  ],

  "predicted": [ ... ],
  ...  
}
```

Note that the processing makes use of Gallia's ["poor man scaling"](https://github.com/galliaproject/gallia-core/blob/init/README.md#poor-man-scaling),
which can be seen in action here in the [processing code](https://github.com/galliaproject/gallia-genemania/blob/init/src/main/scala/galliaexample/genemania/GeneMania.scala#L95).

<a name="references"></a>
### GeneMania References
- __website__: https://genemania.org/
- __publication__: _"The GeneMANIA prediction server: biological network integration for gene prioritization and predicting gene function"; Warde-Farley D, Donaldson SL, Comes O, Zuberi K, Badrawi R, Chao P, Franz M, Grouios C, Kazi F, Lopes CT, Maitland A, Mostafavi S, Montojo J, Shao Q, Wright G, Bader GD, Morris Q; Nucleic Acids Res. 2010 Jul 1;38 Suppl:W214-20 PubMed Abstract (PDF)_

### Contact
You may contact the author at: <sub><img src="./images/ct.png"></sub>
