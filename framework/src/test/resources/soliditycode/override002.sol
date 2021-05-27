

contract A {
    uint public x;
    function setValue(uint _x) public {
        x = _x;
    }
}
contract B is A {}
contract C is A {}
// No explicit override required
contract D is B, C {}