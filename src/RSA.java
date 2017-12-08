import javafx.util.Pair;

import java.util.ArrayList;

public class RSA
{
    private Pair publicKey;
    private Pair privateKey;

    private int n;
    private int phi;
    private int e;
    private int d;

    //Used to randomly generate p & q from file
    public RSA(){

        //Randomly pick p & q from file
        Pair temp = getFromFile();
        int p = (int)temp.getKey();
        int q = (int)temp.getValue();

        //Verify p&q and generate keys
        if(verifyPQ(p,q)){
            generateKeys(p,q);
        }
    }

    //Used when given p & q
    public RSA(int p, int q){

        if(verifyPQ(p,q)){
            generateKeys(p,q);
        }
    }


    //Getters and Setters
    public Pair getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Pair publicKey) {
        this.publicKey = publicKey;
    }

    public Pair getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Pair privateKey) {
        this.privateKey = privateKey;
    }


    //Verify is prime & if large enough
    private boolean verifyPQ(int p, int q){

        //Verify if prime & that p&q are sufficiently large
        if(verifyPrime(p) && verifyPrime(q)){

            double thresh = Math.pow(128,2);
            if(p*q>thresh){
                return true;
            }
            System.out.println("P/Q not big enough");
            return false;
        }
        System.out.println("P or Q are not prime");
        return false;
    }

    //https://www.mkyong.com/java/how-to-determine-a-prime-number-in-java/
    boolean verifyPrime(int x) {
        for(int i=2;i<x;i++) {
            if(x%i==0)
                return false;
        }
        return true;
    }

    //Obtain p & q from file
    private Pair getFromFile(){
        return new Pair(1,1);
    }

    //Generate public private key info
    private void generateKeys(int p, int q){

        //Calculate n
        n = p*q;

        //Calculate phi
        phi = (p-1)*(q-1);

        //Calculate e
        e = calculateE();

        //Calculate d
        d = calculateD();

        publicKey = new Pair(e,n);
        privateKey = new Pair(d,n);
    }

    private int calculateE(){
        for(int i=2;i<n;i++){
            if(gcd(i,phi)==1){
                return i;
            }
        }
        //Should never get here if p & q are prime
        return -1;
    }

    private int calculateD(){
        d = -1;
        for(int i=1;d%2!=0;i++){
            d = (1+(i*phi))/e;
        }
        return d;
    }

    private static int gcd(int a, int b) {
        int t;
        while(b != 0){
            t = a;
            a = b;
            b = t%b;
        }
        return a;
    }

    //Create blocks and encrypt the blocks
    public ArrayList<Long> encrypt(String text){
        int temp_index=0;
        long sum =0;
        ArrayList<Long> blocks = new ArrayList<>();

        if(text.length()%2!=0){
            text=text+"0";
        }

        //Block the message
        for(int i=0;i<text.length();i+=2){
            String temp = text.substring(i,i+2);
            char tempChar1 = temp.charAt(0);
            long asci_val1 = (long) tempChar1;
            long calc1= (long) (asci_val1*(Math.pow(128,0)));

            char tempChar2 = temp.charAt(1);
            long asci_val2 = (long) tempChar2;
            long calc2= (long) (asci_val2*(Math.pow(128,1)));

            sum= calc1+calc2;
            blocks.add(sum);
        }

        //Encrypt each block
        for(int i=0;i<blocks.size();i++){
            long block = blocks.get(i);
            double eTemp = Math.pow(block,e);
            long eBlock = (long)eTemp%n;
            blocks.set(i,eBlock);
            System.out.println(eBlock);
        }

        return blocks;
    }

    //Decrypt and deblock
    public Message decrypt(Message message){

        ArrayList<Long> msg = new ArrayList<>();
        long temp = (long)0;
        msg.add(temp);
        //Decrypt
        for(int i=0;i<msg.size();i++){
            Long block = msg.get(i);
            Long dBlock = (long)(Math.pow(block,d))%n;
            msg.set(i,dBlock);
        }

        //Unblock
        String total = "";
        for(int i=0;i<msg.size();i++){
            long block = msg.get(i);
            char tempChar2 = (char)(block >> 7);

            char tempChar1 = (char)(block - (block>>7)*128);
            total+=tempChar1+tempChar2;
        }

        return message;
    }


    public static void main(String[] args){

        RSA temp = new RSA(10,4919);
        Pair publicKey = temp.getPublicKey();
        Pair privateKey = temp.getPrivateKey();

        System.out.println("E: " + publicKey.getKey());
        System.out.println("N: " + publicKey.getValue());
        System.out.println("D: " + privateKey.getKey());
        System.out.println("N: " + privateKey.getValue());

    }

}

