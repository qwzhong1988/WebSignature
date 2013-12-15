package models;

//@Entity
public class Signature {

  //@Id
  //public Long id;

  //@OneToOne(cascade=CascadeType.ALL)
  //@JoinColumn(name = "user_id")
  public User owner;

  //@EnumMapping(nameValuePairs="TRAINING=TRA,VALIDATION=VAL,TEST=TST,PRODUCTION=PROD")
  public enum Type {
          TRAINING,
          VALIDATION,
          TEST,
          PRODUCTION
  }

  public Type type;

  public String device;

  //@CreatedTimestamp
  //Timestamp cretime;

  public SamplesList samples; 

  public Signature(List<double[]> vectorList, User owner, Type type, String device) {
    this.samples = new SamplesList(vectorList);
    this.owner = owner;
    this.type = type;
    this.device = device;
  }

  public FeatureList extractFeatures() {
    return new FeaturesList(samples);
  }

}