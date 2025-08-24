#include <iostream>
#include <sstream>   // <<<<< necessário para o ostringstream
#include <ctime>

namespace sc
{
    template <typename T>
    std::string toString(T value)
    {
        std::ostringstream oss;
        oss << value;
        return oss.str();
    }

    std::string processTime()
    {
        clock_t start = clock();

        for (long i = 0; i < 100000000; ++i)
        {
        }

        clock_t end = clock();

        double elapsed_secs = double(end - start) / CLOCKS_PER_SEC;

        return toString(elapsed_secs);
    }
}
